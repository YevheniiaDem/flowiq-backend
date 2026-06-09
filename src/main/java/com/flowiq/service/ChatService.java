package com.flowiq.service;

import com.flowiq.config.AppPreferences;
import com.flowiq.dto.request.SendChatMessageRequest;
import com.flowiq.dto.response.ChatConversationResponse;
import com.flowiq.dto.response.ChatMessageResponse;
import com.flowiq.dto.response.SendChatMessageResponse;
import com.flowiq.entity.ChatConversation;
import com.flowiq.entity.ChatMessage;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.ChatConversationRepository;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import com.flowiq.util.CurrencyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionSeedService transactionSeedService;

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getConversations() {
        User user = getCurrentUserEntity();
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(ChatConversationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse getConversation(Long conversationId) {
        User user = getCurrentUserEntity();
        ChatConversation conversation = conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return ChatConversationResponse.fromEntity(conversation);
    }

    @Transactional
    public SendChatMessageResponse sendMessage(SendChatMessageRequest request) {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        ChatConversation conversation = resolveConversation(user, request);
        String trimmedMessage = request.getMessage().trim();

        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setRole(ChatMessage.Role.USER);
        userMessage.setContent(trimmedMessage);
        conversation.getMessages().add(userMessage);

        String aiReply = generateReply(user, trimmedMessage);
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(ChatMessage.Role.ASSISTANT);
        assistantMessage.setContent(aiReply);
        conversation.getMessages().add(assistantMessage);

        conversationRepository.save(conversation);

        return SendChatMessageResponse.builder()
                .conversationId(String.valueOf(conversation.getId()))
                .userMessage(ChatMessageResponse.fromEntity(userMessage))
                .assistantMessage(ChatMessageResponse.fromEntity(assistantMessage))
                .build();
    }

    private ChatConversation resolveConversation(User user, SendChatMessageRequest request) {
        if (request.getConversationId() != null) {
            return conversationRepository.findByIdAndUserId(request.getConversationId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setUser(user);
        conversation.setTitle(truncateTitle(request.getMessage()));
        return conversation;
    }

    private String truncateTitle(String message) {
        String title = message.trim();
        return title.length() > 60 ? title.substring(0, 57) + "..." : title;
    }

    private String generateReply(User user, String message) {
        boolean uk = AppPreferences.current().isUkrainian();
        String lower = message.toLowerCase(Locale.ROOT);

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        BigDecimal currentRevenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal previousRevenue = sum(user.getId(), Transaction.Type.REVENUE, previous);
        BigDecimal currentExpenses = sum(user.getId(), Transaction.Type.EXPENSE, current);
        BigDecimal previousExpenses = sum(user.getId(), Transaction.Type.EXPENSE, previous);
        BigDecimal profit = currentRevenue.subtract(currentExpenses);
        BigDecimal cashFlow = profit.multiply(new BigDecimal("0.90")).setScale(0, RoundingMode.HALF_UP);

        double revenueChange = percentChange(currentRevenue, previousRevenue);
        double expenseChange = percentChange(currentExpenses, previousExpenses);

        if (matches(lower, "дохід", "виручк", "revenue", "sales", "продаж")) {
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Дохід за поточний місяць: %s (%+.1f%% до минулого місяця). Найбільші джерела: %s.",
                    CurrencyFormatter.format(currentRevenue), revenueChange, topRevenueCategories(user, current))
                    : String.format(Locale.US,
                    "Current month revenue: %s (%+.1f%% vs last month). Top sources: %s.",
                    CurrencyFormatter.format(currentRevenue), revenueChange, topRevenueCategories(user, current));
        }

        if (matches(lower, "витрат", "expense", "cost", "витрати")) {
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Витрати за поточний місяць: %s (%+.1f%% до минулого місяця). Найбільші категорії: %s.",
                    CurrencyFormatter.format(currentExpenses), expenseChange, topExpenseCategories(user, current))
                    : String.format(Locale.US,
                    "Current month expenses: %s (%+.1f%% vs last month). Top categories: %s.",
                    CurrencyFormatter.format(currentExpenses), expenseChange, topExpenseCategories(user, current));
        }

        if (matches(lower, "прибут", "profit", "марж")) {
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Прибуток за поточний місяць: %s. Дохід %s, витрати %s.",
                    CurrencyFormatter.format(profit),
                    CurrencyFormatter.format(currentRevenue),
                    CurrencyFormatter.format(currentExpenses))
                    : String.format(Locale.US,
                    "Current month profit: %s. Revenue %s, expenses %s.",
                    CurrencyFormatter.format(profit),
                    CurrencyFormatter.format(currentRevenue),
                    CurrencyFormatter.format(currentExpenses));
        }

        if (matches(lower, "грошов", "cash flow", "cashflow", "касов", "ліквідн")) {
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Оціночний грошовий потік цього місяця: %s. Це ~90%% від прибутку (%s).",
                    CurrencyFormatter.format(cashFlow), CurrencyFormatter.format(profit))
                    : String.format(Locale.US,
                    "Estimated cash flow this month: %s. That's ~90%% of profit (%s).",
                    CurrencyFormatter.format(cashFlow), CurrencyFormatter.format(profit));
        }

        if (matches(lower, "прогноз", "forecast", "квартал", "quarter", "майбут")) {
            BigDecimal forecast = currentRevenue.multiply(new BigDecimal("1.08"))
                    .setScale(0, RoundingMode.HALF_UP);
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "На основі поточної динаміки (дохід %+.1f%%) прогнозуємо дохід наступного місяця близько %s. Рекомендую утримувати витрати в межах %s.",
                    revenueChange, CurrencyFormatter.format(forecast),
                    CurrencyFormatter.format(currentExpenses.multiply(new BigDecimal("0.95")).setScale(0, RoundingMode.HALF_UP)))
                    : String.format(Locale.US,
                    "Based on current momentum (revenue %+.1f%%), next month's revenue is projected around %s. I recommend keeping expenses near %s.",
                    revenueChange, CurrencyFormatter.format(forecast),
                    CurrencyFormatter.format(currentExpenses.multiply(new BigDecimal("0.95")).setScale(0, RoundingMode.HALF_UP)));
        }

        if (matches(lower, "маркетинг", "marketing", "реклам", "roi")) {
            BigDecimal marketing = sumCategory(user, current, "Marketing");
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Витрати на маркетинг цього місяця: %s. Це %.1f%% від загальних витрат. Якщо дохід зростає повільніше за маркетинг — варто переглянути ROI кампаній.",
                    CurrencyFormatter.format(marketing),
                    toPercent(marketing, currentExpenses))
                    : String.format(Locale.US,
                    "Marketing spend this month: %s. That's %.1f%% of total expenses. If revenue grows slower than marketing, review campaign ROI.",
                    CurrencyFormatter.format(marketing),
                    toPercent(marketing, currentExpenses));
        }

        if (matches(lower, "зниз", "зменш", "decrease", "down", "падін", "чому")) {
            if (revenueChange < 0) {
                return uk
                        ? String.format(Locale.forLanguageTag("uk-UA"),
                        "Дохід знизився на %.1f%% порівняно з минулим місяцем (%s → %s). Перевірте канали продажів та сезонність.",
                        Math.abs(revenueChange),
                        CurrencyFormatter.format(previousRevenue),
                        CurrencyFormatter.format(currentRevenue))
                        : String.format(Locale.US,
                        "Revenue decreased by %.1f%% compared to last month (%s → %s). Review sales channels and seasonality.",
                        Math.abs(revenueChange),
                        CurrencyFormatter.format(previousRevenue),
                        CurrencyFormatter.format(currentRevenue));
            }
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Дохід фактично зріс на %+.1f%%. Можливо, ви мали на увазі витрати (%+.1f%%) або конкретну категорію?",
                    revenueChange, expenseChange)
                    : String.format(Locale.US,
                    "Revenue actually grew by %+.1f%%. Perhaps you meant expenses (%+.1f%%) or a specific category?",
                    revenueChange, expenseChange);
        }

        return uk
                ? String.format(Locale.forLanguageTag("uk-UA"),
                "Ось короткий огляд вашого бізнесу: дохід %s (%+.1f%%), витрати %s (%+.1f%%), прибуток %s. Запитайте про дохід, витрати, прибуток, грошовий потік або прогноз.",
                CurrencyFormatter.format(currentRevenue), revenueChange,
                CurrencyFormatter.format(currentExpenses), expenseChange,
                CurrencyFormatter.format(profit))
                : String.format(Locale.US,
                "Here's a quick overview: revenue %s (%+.1f%%), expenses %s (%+.1f%%), profit %s. Ask me about revenue, expenses, profit, cash flow, or forecasts.",
                CurrencyFormatter.format(currentRevenue), revenueChange,
                CurrencyFormatter.format(currentExpenses), expenseChange,
                CurrencyFormatter.format(profit));
    }

    private boolean matches(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String topExpenseCategories(User user, YearMonth month) {
        return transactionRepository.sumExpensesByCategory(
                        user.getId(), month.atDay(1), month.atEndOfMonth()).stream()
                .limit(3)
                .map(row -> row.getCategory() + " (" + CurrencyFormatter.format(row.getAmount()) + ")")
                .collect(Collectors.joining(", "));
    }

    private String topRevenueCategories(User user, YearMonth month) {
        return transactionRepository.sumRevenueByCategory(
                        user.getId(), month.atDay(1), month.atEndOfMonth()).stream()
                .limit(3)
                .map(row -> row.getCategory() + " (" + CurrencyFormatter.format(row.getAmount()) + ")")
                .collect(Collectors.joining(", "));
    }

    private BigDecimal sumCategory(User user, YearMonth month, String category) {
        return transactionRepository.sumExpensesByCategory(
                        user.getId(), month.atDay(1), month.atEndOfMonth()).stream()
                .filter(row -> category.equalsIgnoreCase(row.getCategory()))
                .map(TransactionRepository.CategorySumProjection::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private double toPercent(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private BigDecimal sum(Long userId, Transaction.Type type, YearMonth month) {
        return transactionRepository.sumByUserAndTypeAndDateRange(
                userId, type, month.atDay(1), month.atEndOfMonth());
    }

    private double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
