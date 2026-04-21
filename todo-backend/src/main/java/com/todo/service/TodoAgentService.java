package com.todo.service;

import com.todo.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

@Service
public class TodoAgentService {

    private final ChatClient chatClient;
    private final TodoService todoService;
    private final UserService userService;
    private final FriendService friendService;

    private final Map<String, AgentSession> sessions = new HashMap<>();

    private final String SYSTEM_PROMPT = "你是一个 Todo List 管理 Agent，专门帮助用户管理任务。你需要根据用户的自然语言输入，理解其意图，并按照以下规定的 XML 格式输出。\n" +
            "\n" +
            "## 任务数据模型（对应数据库 todos 表）\n" +
            "每个任务包含以下属性：\n" +
            "- 任务id (id): 唯一标识，整数（Long），自增主键\n" +
            "- 任务名 (title): 简短标题（必填）\n" +
            "- 任务内容 (description): 详细描述（尽量填写，可由 Agent 根据上下文生成）\n" +
            "- 截止时间 (dueDate): 格式 \"YYYY-MM-DD HH:MM\"（24小时制）；若未提供时间则默认为 \"00:00\"\n" +
            "- 紧急程度 (priority): 字符串，取值 \"轻松\"、\"中等\"、\"紧急\"（必填，默认 \"中等\"）\n" +
            "- 是否完成 (completed): 布尔值，true / false（默认 false）\n" +
            "- 是否删除 (deleted): 布尔值，true / false（默认 false）\n" +
            "- 创建时间 (createdAt): 系统自动生成，无需 Agent 填写\n" +
            "- 完成时间 (completedAt): 系统自动生成，无需 Agent 填写\n" +
            "- 创建用户ID (user_id): 当前用户 ID（自动绑定）\n" +
            "- 目标用户ID (target_user_id): 用于任务派发，若为好友任务则填写对方 ID\n" +
            "- 是否已接受 (accepted): 布尔值，仅对派发给自己的任务有意义，默认 false\n" +
            "\n" +
            "## 可用 Skill（你只能调用以下8种）\n" +
            "所有 Skill 调用后系统都会执行操作并返回结果。你每个输出中最多只能调用一个 Skill，且不能与 <answer> 同时出现。\n" +
            "\n" +
            "1. **get_tasks** - 获取当前用户的任务列表（包括自己创建的和派发给自己的，未删除的）\n" +
            "   参数: 无\n" +
            "   调用格式: <get_tasks />\n" +
            "\n" +
            "2. **get_friends** - 获取当前用户的好友列表（返回 id, username, name 等）\n" +
            "   参数: 无\n" +
            "   调用格式: <get_friends />\n" +
            "\n" +
            "3. **create_task** - 创建自己的新任务\n" +
            "   参数: title(必填), description(尽量填写), dueDate(可选), priority(可选，默认\"中等\")\n" +
            "   调用格式: <create_task title=\"买牛奶\" description=\"去超市买一箱低脂牛奶\" dueDate=\"2026-04-15 19:30\" priority=\"紧急\" />\n" +
            "\n" +
            "4. **complete_task** - 完成任务（设置 completed=true 和 completedAt=当前时间）\n" +
            "   参数: id(必填)\n" +
            "   调用格式: <complete_task id=\"3\" />\n" +
            "\n" +
            "5. **delete_task** - 软删除任务（设置 deleted=true）\n" +
            "   参数: id(必填)\n" +
            "   调用格式: <delete_task id=\"5\" />\n" +
            "\n" +
            "6. **add_friend_task** - 添加好友分配的任务（创建一个 target_user_id 为好友 ID 的任务，且 accepted=false）\n" +
            "   参数: title(必填), description(尽量填写), dueDate(可选), priority(可选), friend_name(必填，好友的用户名或姓名)\n" +
            "   调用格式: <add_friend_task title=\"准备会议材料\" description=\"整理Q2销售数据PPT\" friend_name=\"张三\" dueDate=\"2026-04-16 09:00\" priority=\"紧急\" />\n" +
            "\n" +
            "7. **update_task** - 更新任务属性（可修改 title, description, dueDate, priority）\n" +
            "   参数: id(必填), title(可选), description(可选), dueDate(可选), priority(可选)\n" +
            "   调用格式: <update_task id=\"2\" dueDate=\"2026-04-20 18:00\" priority=\"紧急\" />\n" +
            "\n" +
            "8. **accept_task** - 接受派发给自己的任务（设置 accepted=true）\n" +
            "   参数: id(必填)\n" +
            "   调用格式: <accept_task id=\"7\" />\n" +
            "\n" +
            "## 输出格式要求\n" +
            "你必须严格按照以下格式输出，且输出必须是纯 XML 文本（无 Markdown 包裹）：\n" +
            "\n" +
            "<THINK> 标签内分析用户意图，提取关键信息，并确定要调用的 Skill 或是否输出 <answer>。\n" +
            "\n" +
            "然后，根据情况选择以下两种之一输出（不能同时）：\n" +
            "- 选项 A（执行操作）：输出一个 Skill 标签（例如 <create_task ... />），不输出 <answer>。系统执行后会返回结果，Agent 将自动获得返回值并决定下一步。**注意：调用 Skill 后只会收到系统的调用结果，不会收到用户的新消息。**\n" +
            "- 选项 B（纯对话）：输出 <answer> 标签，不输出任何 Skill。仅用于打招呼、帮助信息、或告知无法处理（但不要求用户再输入）。**注意：输出 <answer> 后才有可能收到用户的新回答。**\n" +
            "\n" +
            "## 重要规则\n" +
            "\n" +
            "### 规则1：严禁在 <answer> 中要求用户再次输入或确认\n" +
            "因为用户可能不再输入，且系统会根据 Skill 返回值自动继续对话。\n" +
            "\n" +
            "### 规则2：对删除/修改/完成任务/接受任务的模糊请求（如“删除买菜任务”、“完成最紧急的任务”、“接受任务3”）\n" +
            "- **如果当前上下文中已有任务列表**（例如刚刚调用过 get_tasks 且获得了返回结果），你必须直接推断出最可能的 id，并输出对应的 Skill（delete_task、complete_task、update_task 或 accept_task）。\n" +
            "- **如果当前没有任务列表**（即不知道任何任务的 ID 和标题），你不得凭空推断。必须先调用 <get_tasks /> 获取列表，然后系统会返回结果，你将在下一轮根据返回的任务列表再次处理用户的原始请求。\n" +
            "\n" +
            "### 规则3：创建任务时，description 的生成策略（按优先级）\n" +
            "1. 优先从用户的原话中提取相关内容（如“买一箱低脂牛奶”）。\n" +
            "2. 若没有明确描述，根据任务名生成具体合理的描述：\n" +
            "   - 对于购物类：“购买 [任务标题]”\n" +
            "   - 对于工作类：“完成 [任务标题]”\n" +
            "   - 对于学习类：“学习 [任务标题] 相关内容”\n" +
            "   - 对于运动类：“进行 [任务标题] 锻炼”\n" +
            "3. 兜底描述：“按时完成该任务”\n" +
            "\n" +
            "### 规则4：缺失必要参数\n" +
            "如果用户请求缺少必要参数（例如创建任务没有任务标题），你无法直接调用 Skill，此时只能输出 <answer> 说明情况，但不能要求用户补充。\n" +
            "\n" +
            "### 规则5：需要多个 Skill 时的处理\n" +
            "如果用户请求需要多个 Skill，你只输出第一个 Skill。系统执行后返回结果，Agent 会根据返回值自动发起下一轮调用。\n" +
            "\n" +
            "**例外：批量创建结构化任务**  \n" +
            "当用户明确请求创建一组结构化的任务（如“安排一周的跑步计划”、“制定每日学习计划”等），并且 Agent 有足够信息自动拆解成多个任务时，Agent 应当自动连续调用 create_task 或 add_friend_task。  \n" +
            "- 外部系统会循环调用 Agent：每次执行完一个 Skill 后，将结果返回给 Agent，Agent 根据内部计划决定是否继续输出下一个 Skill。\n" +
            "- Agent 需要维护一个内部计划（例如任务列表、当前索引），在每次输出时检查是否还有剩余任务。\n" +
            "- 如果还有任务未创建，继续输出下一个 create_task；如果全部完成，最后输出一个 <answer> 总结。\n" +
            "\n" +
            "### 规则6：当前日期\n" +
            "当前日期由系统动态提供，格式为 YYYY-MM-DD。在编写本提示词时，使用 {{current_date}} 占位符，实际调用时系统会替换为真实日期（例如 2026-04-14）。你需要基于该日期解析用户的相对时间（如“明天下午3点” → 明天 15:00）。\n" +
            "\n" +
            "### 规则7：优先级与截止时间格式\n" +
            "- 优先级必须是“轻松”、“中等”、“紧急”之一，默认“中等”。\n" +
            "- 截止时间精确到分钟，格式 YYYY-MM-DD HH:MM。若用户只给日期，默认时间 00:00；若说“下午3点”则 15:00；“今晚8点半”则为 20:30。\n" +
            "\n" +
            "### 规则8：好友相关操作\n" +
            "- 当用户提及好友姓名但你不确定该好友是否存在时，应先调用 <get_friends /> 获取好友列表进行确认。\n" +
            "- 添加好友任务（add_friend_task）中的 friend_name 必须是已存在的好友的用户名或姓名。若用户提供的好友名不在列表中，你可以先调用 get_friends 获取列表后再处理，或输出 <answer> 告知用户该好友不存在。\n" +
            "- 当用户要求“接受任务”时，若任务列表中有派发给自己的任务（accepted=false），直接调用 accept_task；否则提示没有待接受的任务。\n" +
            "\n" +
            "### 规则9：任务状态说明\n" +
            "- 任务是否完成 (completed) 和是否删除 (deleted) 是两个独立维度。已完成但未删除的任务仍会显示，但标记为已完成。\n" +
            "- 派发给自己的任务 (target_user_id 为当前用户) 初始 accepted=false，需要用户明确接受后才算真正认领。未接受的任务可能不会计入某些统计，但仍可见。\n" +
            "\n" +
            "## 示例\n" +
            "\n" +
            "### 示例1：用户说“显示我所有的任务”\n" +
            "<THINK>用户想要查看任务列表，调用 get_tasks。</THINK>\n" +
            "<get_tasks />\n" +
            "\n" +
            "### 示例2：用户说“显示我的好友列表”\n" +
            "<THINK>用户想要查看好友列表，调用 get_friends。</THINK>\n" +
            "<get_friends />\n" +
            "\n" +
            "### 示例3：用户说“帮我添加一个任务：明天下午3点前写完报告，紧急”\n" +
            "<THINK>用户要创建任务，任务标题“写完报告”，截止时间明天15:00，优先级紧急。根据描述生成策略（工作类）生成描述“完成项目总结报告”。</THINK>\n" +
            "<create_task title=\"写完报告\" description=\"完成项目总结报告\" dueDate=\"2026-04-15 15:00\" priority=\"紧急\" />\n" +
            "\n" +
            "### 示例4：用户说“添加任务：买生日礼物”\n" +
            "<THINK>用户创建任务“买生日礼物”，根据购物类生成描述“购买生日礼物”。</THINK>\n" +
            "<create_task title=\"买生日礼物\" description=\"购买生日礼物\" />\n" +
            "\n" +
            "### 示例5：用户说“把ID为12的任务标记为完成”\n" +
            "<THINK>用户明确给出 id=12，调用 complete_task。</THINK>\n" +
            "<complete_task id=\"12\" />\n" +
            "\n" +
            "### 示例6：用户说“删除任务7”\n" +
            "<THINK>用户要求删除任务7，调用 delete_task。</THINK>\n" +
            "<delete_task id=\"7\" />\n" +
            "\n" +
            "### 示例7：用户说“删除买菜任务”（当前没有任务列表）\n" +
            "<THINK>用户想删除标题为“买菜”的任务，但当前不知道任务列表，无法推断 id。先调用 get_tasks 获取列表，下一轮再处理删除。</THINK>\n" +
            "<get_tasks />\n" +
            "\n" +
            "### 示例8：用户说“完成最紧急的任务”（已有任务列表）\n" +
            "<THINK>根据已有任务列表，推断最紧急未完成任务 id=2，调用 complete_task。</THINK>\n" +
            "<complete_task id=\"2\" />\n" +
            "\n" +
            "### 示例9：用户说“把第一个任务完成”（已有任务列表）\n" +
            "<THINK>推断第一个任务 id=1，调用 complete_task。</THINK>\n" +
            "<complete_task id=\"1\" />\n" +
            "\n" +
            "### 示例10：用户说“我朋友小红让我帮忙设计一个logo，今天之内”\n" +
            "<THINK>好友任务，任务标题“设计logo”，好友小红，截止今天23:59，生成描述“按要求完成logo设计”。假设小红在好友列表中。</THINK>\n" +
            "<add_friend_task title=\"设计logo\" description=\"按要求完成logo设计\" friend_name=\"小红\" dueDate=\"2026-04-14 23:59\" />\n" +
            "\n" +
            "### 示例11：用户说“把任务3的截止时间改成后天上午10点”\n" +
            "<THINK>更新任务3的截止时间，计算后天日期为 {{current_date+2}} 10:00，调用 update_task。</THINK>\n" +
            "<update_task id=\"3\" dueDate=\"2026-04-16 10:00\" />\n" +
            "\n" +
            "### 示例12：用户说“接受派发给我的任务5”\n" +
            "<THINK>用户明确要接受 id=5 的任务，调用 accept_task。</THINK>\n" +
            "<accept_task id=\"5\" />\n" +
            "\n" +
            "### 示例13：用户说“你好”\n" +
            "<THINK>用户打招呼，无任务操作，输出 answer。</THINK>\n" +
            "<answer>你好！我是 Todo List 助手，你可以让我创建、查看、完成、更新、删除或接受任务，也可以添加好友分配的任务。</answer>\n" +
            "\n" +
            "### 示例14：用户说“创建一个任务”（缺少任务标题）\n" +
            "<THINK>用户想创建任务但没有提供标题，无法调用 Skill，输出 answer 说明。</THINK>\n" +
            "<answer>无法创建任务：缺少任务标题。</answer>\n" +
            "\n" +
            "## 核心原则\n" +
            "- 完成用户要求后必须输出 <answer> 标签。不可以出现什么都不输出等待用户输入的情况。\n" +
            "- 输出中只能有 <THINK> + 一个 Skill 或者 <THINK> + <answer>，禁止同时出现 Skill 和 <answer>。\n" +
            "- 禁止在 <answer> 中要求用户再次输入。\n" +
            "- 不要输出任何非 XML 内容，不要使用 Markdown 代码块。\n" +
            "当前日期: ";

    @Autowired
    public TodoAgentService(ChatClient.Builder builder, TodoService todoService,
                            UserService userService, FriendService friendService) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT + currentDate)
                .build();
        this.todoService = todoService;
        this.userService = userService;
        this.friendService = friendService;
    }

    public AgentChatResponse processUserMessage(String userMessage, User currentUser) {
        String userId = currentUser.getId().toString();
        AgentSession session = sessions.computeIfAbsent(userId, k -> new AgentSession());
        session.setUserId(userId);

        session.getChatHistory().add(new UserMessage(userMessage));

        List<String> allThinkContents = new ArrayList<>();
        String finalAnswer = "";

        try {
            String currentInput = userMessage;
            int maxIterations = 10;
            int iterations = 0;

            while (iterations < maxIterations) {
                iterations++;
                String aiResponse = chatClient.prompt()
                        .messages(session.getChatHistory())
                        .call()
                        .content();

                session.getChatHistory().add(new AssistantMessage(aiResponse));

                String thinkContent = extractThinkContent(aiResponse);
                if (thinkContent != null) {
                    allThinkContents.add(thinkContent);
                }

                String responseForParsing = aiResponse.replaceAll("(?i)<THINK>.*?</THINK>", "");

                AgentAction action = parseAgentResponse(responseForParsing);

                if ("answer".equals(action.getType())) {
                    finalAnswer = action.getAnswer();
                    session.getChatHistory().add(new AssistantMessage(finalAnswer));
                    break;
                } else {
                    String skillResultJson = executeSkillAndReturnJson(action, currentUser, session);
                    currentInput = "技能执行结果：" + skillResultJson;
                    session.getChatHistory().add(new UserMessage(currentInput));
                }
            }

            if (iterations >= maxIterations) {
                finalAnswer = "抱歉，处理超时，请重试。";
                session.getChatHistory().add(new AssistantMessage(finalAnswer));
            }
        } catch (Exception e) {
            e.printStackTrace();
            finalAnswer = "抱歉，处理您的请求时出错了：" + e.getMessage();
        }

        String allThinkContent = String.join("\n---\n", allThinkContents);
        return new AgentChatResponse(finalAnswer, allThinkContent);
    }

    private String executeSkillAndReturnJson(AgentAction action, User currentUser, AgentSession session) {
        Map<String, Object> result = new HashMap<>();
        result.put("skillType", action.getType());
        result.put("success", true);

        try {
            switch (action.getType()) {
                case "get_tasks":
                    List<Todo> tasks = todoService.getTodosByUser(currentUser);
                    session.setCurrentTasks(tasks);
                    List<Map<String, Object>> taskList = new ArrayList<>();
                    for (Todo todo : tasks) {
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("id", todo.getId());
                        taskMap.put("title", todo.getTitle());
                        taskMap.put("description", todo.getDescription());
                        taskMap.put("priority", todo.getPriority());
                        taskMap.put("completed", todo.isCompleted());
                        taskMap.put("accepted", todo.isAccepted());
                        if (todo.getDueDate() != null) {
                            taskMap.put("dueDate", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(todo.getDueDate()));
                        }
                        taskList.add(taskMap);
                    }
                    result.put("tasks", taskList);
                    result.put("message", "获取任务列表成功，共 " + tasks.size() + " 个任务");
                    break;

                case "get_friends":
                    List<User> friends = friendService.getFriends(currentUser);
                    session.setCurrentFriends(friends);
                    List<Map<String, Object>> friendList = new ArrayList<>();
                    for (User friend : friends) {
                        Map<String, Object> friendMap = new HashMap<>();
                        friendMap.put("id", friend.getId());
                        friendMap.put("username", friend.getUsername());
                        friendMap.put("name", friend.getName());
                        friendList.add(friendMap);
                    }
                    result.put("friends", friendList);
                    result.put("message", "获取好友列表成功，共 " + friends.size() + " 个好友");
                    break;

                case "create_task":
                    if (action.getTitle() == null || action.getTitle().isEmpty()) {
                        result.put("success", false);
                        result.put("error", "缺少任务标题");
                        break;
                    }
                    String description = action.getDescription();
                    if (description == null || description.isEmpty()) {
                        description = generateDescription(action.getTitle());
                    }
                    Todo newTodo = new Todo();
                    newTodo.setTitle(action.getTitle());
                    newTodo.setDescription(description);
                    if (action.getDueDate() != null) {
                        try {
                            newTodo.setDueDate(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(action.getDueDate()));
                        } catch (Exception e) {
                            try {
                                newTodo.setDueDate(new SimpleDateFormat("yyyy-MM-dd").parse(action.getDueDate()));
                            } catch (Exception e2) {
                            }
                        }
                    }
                    newTodo.setPriority(action.getPriority() != null ? action.getPriority() : "中等");
                    newTodo.setCompleted(false);
                    newTodo.setDeleted(false);
                    newTodo.setAccepted(true);
                    Todo createdTodo = todoService.createTodo(newTodo, currentUser);
                    result.put("createdTask", toMap(createdTodo));
                    result.put("message", "任务创建成功");
                    break;

                case "complete_task":
                    if (action.getId() == null) {
                        result.put("success", false);
                        result.put("error", "缺少任务 id");
                        break;
                    }
                    Todo completedTodo = todoService.toggleTodoStatus(action.getId(), currentUser);
                    if (completedTodo != null) {
                        result.put("completedTask", toMap(completedTodo));
                        result.put("message", "任务完成");
                    } else {
                        result.put("success", false);
                        result.put("error", "未找到任务");
                    }
                    break;

                case "delete_task":
                    if (action.getId() == null) {
                        result.put("success", false);
                        result.put("error", "缺少任务 id");
                        break;
                    }
                    Todo todoToDelete = todoService.getTodoById(action.getId(), currentUser);
                    if (todoToDelete != null) {
                        todoService.deleteTodo(action.getId(), currentUser);
                        result.put("deletedTask", toMap(todoToDelete));
                        result.put("message", "任务已删除");
                    } else {
                        result.put("success", false);
                        result.put("error", "未找到任务");
                    }
                    break;

                case "add_friend_task":
                    if (action.getTitle() == null || action.getTitle().isEmpty()) {
                        result.put("success", false);
                        result.put("error", "缺少任务标题");
                        break;
                    }
                    if (action.getFriendName() == null || action.getFriendName().isEmpty()) {
                        result.put("success", false);
                        result.put("error", "缺少好友名");
                        break;
                    }
                    User targetFriend = findFriendByName(action.getFriendName(), currentUser, session);
                    if (targetFriend == null) {
                        result.put("success", false);
                        result.put("error", "未找到好友");
                        break;
                    }
                    Todo friendTodo = new Todo();
                    friendTodo.setTitle(action.getTitle());
                    friendTodo.setDescription(action.getDescription() != null ? action.getDescription() : "按时完成该任务");
                    if (action.getDueDate() != null) {
                        try {
                            friendTodo.setDueDate(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(action.getDueDate()));
                        } catch (Exception e) {
                            try {
                                friendTodo.setDueDate(new SimpleDateFormat("yyyy-MM-dd").parse(action.getDueDate()));
                            } catch (Exception e2) {
                            }
                        }
                    }
                    friendTodo.setPriority(action.getPriority() != null ? action.getPriority() : "中等");
                    friendTodo.setTargetUserId(targetFriend.getId());
                    friendTodo.setCompleted(false);
                    friendTodo.setDeleted(false);
                    friendTodo.setAccepted(false);
                    Todo createdFriendTodo = todoService.postTaskToFriend(friendTodo, currentUser);
                    result.put("addedFriendTask", toMap(createdFriendTodo));
                    result.put("targetFriend", toMap(targetFriend));
                    result.put("message", "任务已分配给好友");
                    break;

                case "update_task":
                    if (action.getId() == null) {
                        result.put("success", false);
                        result.put("error", "缺少任务 id");
                        break;
                    }
                    Todo existingTodo = todoService.getTodoById(action.getId(), currentUser);
                    if (existingTodo == null) {
                        result.put("success", false);
                        result.put("error", "未找到任务");
                        break;
                    }
                    if (action.getTitle() != null) existingTodo.setTitle(action.getTitle());
                    if (action.getDescription() != null) existingTodo.setDescription(action.getDescription());
                    if (action.getPriority() != null) existingTodo.setPriority(action.getPriority());
                    if (action.getDueDate() != null) {
                        try {
                            existingTodo.setDueDate(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(action.getDueDate()));
                        } catch (Exception e) {
                            try {
                                existingTodo.setDueDate(new SimpleDateFormat("yyyy-MM-dd").parse(action.getDueDate()));
                            } catch (Exception e2) {
                            }
                        }
                    }
                    Todo updatedTodo = todoService.updateTodo(action.getId(), existingTodo, currentUser);
                    result.put("updatedTask", toMap(updatedTodo));
                    result.put("message", "任务更新成功");
                    break;

                case "accept_task":
                    if (action.getId() == null) {
                        result.put("success", false);
                        result.put("error", "缺少任务 id");
                        break;
                    }
                    Todo acceptedTodo = todoService.acceptTask(action.getId(), currentUser);
                    if (acceptedTodo != null) {
                        result.put("acceptedTask", toMap(acceptedTodo));
                        result.put("message", "任务已接受");
                    } else {
                        result.put("success", false);
                        result.put("error", "未找到任务或该任务不是派发给你的");
                    }
                    break;

                default:
                    result.put("success", false);
                    result.put("error", "未知的技能类型");
                    break;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return toJson(result);
    }

    private Map<String, Object> toMap(Todo todo) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", todo.getId());
        map.put("title", todo.getTitle());
        map.put("description", todo.getDescription());
        map.put("priority", todo.getPriority());
        map.put("completed", todo.isCompleted());
        map.put("accepted", todo.isAccepted());
        if (todo.getDueDate() != null) {
            map.put("dueDate", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(todo.getDueDate()));
        }
        return map;
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("name", user.getName());
        return map;
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\": ");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(escapeJson((String) entry.getValue())).append("\"");
            } else if (entry.getValue() instanceof Number || entry.getValue() instanceof Boolean) {
                sb.append(entry.getValue());
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(entry.getValue()))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String extractThinkContent(String response) {
        Pattern pattern = Pattern.compile("<THINK>(.*?)</THINK>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private AgentAction parseAgentResponse(String response) {
        AgentAction action = new AgentAction();

        // 先尝试匹配完整闭合的 answer 标签
        Pattern answerPattern = Pattern.compile("<answer>(.*?)</answer>", Pattern.DOTALL);
        Matcher answerMatcher = answerPattern.matcher(response);
        if (answerMatcher.find()) {
            action.setType("answer");
            action.setAnswer(answerMatcher.group(1).trim());
            return action;
        }

        if (response.contains("<get_tasks")) {
            action.setType("get_tasks");
        } else if (response.contains("<get_friends")) {
            action.setType("get_friends");
        } else if (response.contains("<create_task")) {
            action.setType("create_task");
            action.setTitle(extractAttribute(response, "title"));
            action.setDescription(extractAttribute(response, "description"));
            action.setDueDate(extractAttribute(response, "dueDate"));
            action.setPriority(extractAttribute(response, "priority"));
        } else if (response.contains("<complete_task")) {
            action.setType("complete_task");
            String idStr = extractAttribute(response, "id");
            if (idStr != null) {
                action.setId(Long.parseLong(idStr));
            }
        } else if (response.contains("<delete_task")) {
            action.setType("delete_task");
            String idStr = extractAttribute(response, "id");
            if (idStr != null) {
                action.setId(Long.parseLong(idStr));
            }
        } else if (response.contains("<add_friend_task")) {
            action.setType("add_friend_task");
            action.setTitle(extractAttribute(response, "title"));
            action.setDescription(extractAttribute(response, "description"));
            action.setDueDate(extractAttribute(response, "dueDate"));
            action.setPriority(extractAttribute(response, "priority"));
            String friendName = extractAttribute(response, "friend_name");
            if (friendName == null) {
                friendName = extractAttribute(response, "friendName");
            }
            action.setFriendName(friendName);
        } else if (response.contains("<update_task")) {
            action.setType("update_task");
            String idStr = extractAttribute(response, "id");
            if (idStr != null) {
                action.setId(Long.parseLong(idStr));
            }
            action.setTitle(extractAttribute(response, "title"));
            action.setDescription(extractAttribute(response, "description"));
            action.setDueDate(extractAttribute(response, "dueDate"));
            action.setPriority(extractAttribute(response, "priority"));
        } else if (response.contains("<accept_task")) {
            action.setType("accept_task");
            String idStr = extractAttribute(response, "id");
            if (idStr != null) {
                action.setId(Long.parseLong(idStr));
            }
        } else {
            action.setType("answer");
            action.setAnswer(response);
        }

        return action;
    }

    private String extractAttribute(String xml, String attrName) {
        Pattern pattern = Pattern.compile(attrName + "=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String generateDescription(String title) {
        title = title.toLowerCase();
        if (title.contains("买") || title.contains("购物") || title.contains("超市")) {
            return "购买 " + title;
        } else if (title.contains("工作") || title.contains("报告") || title.contains("项目") || title.contains("任务")) {
            return "完成 " + title;
        } else if (title.contains("学习") || title.contains("读书") || title.contains("课程")) {
            return "学习 " + title + " 相关内容";
        } else if (title.contains("跑") || title.contains("运动") || title.contains("锻炼") || title.contains("健身")) {
            return "进行 " + title + " 锻炼";
        }
        return "按时完成该任务";
    }

    private User findFriendByName(String name, User currentUser, AgentSession session) {
        List<User> friends;
        if (session.getCurrentFriends() != null) {
            friends = session.getCurrentFriends();
        } else {
            friends = friendService.getFriends(currentUser);
            session.setCurrentFriends(friends);
        }

        for (User friend : friends) {
            if (friend.getName() != null && friend.getName().equals(name)) {
                return friend;
            }
            if (friend.getUsername() != null && friend.getUsername().equals(name)) {
                return friend;
            }
        }
        return null;
    }
}
