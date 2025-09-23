import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelegramConfigTest {
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.chat.id}")
    private String chatId;
    
    public void testConfiguration() {
        System.out.println("🔍 Testing Telegram Configuration...");
        System.out.println("📱 Bot Token: " + (botToken != null ? botToken.substring(0, 10) + "..." : "null"));
        System.out.println("💬 Chat ID: " + chatId);
        
        if (botToken != null && !botToken.isEmpty() && !botToken.equals("your_bot_token")) {
            System.out.println("✅ Bot Token is configured");
        } else {
            System.out.println("❌ Bot Token is not configured properly");
        }
        
        if (chatId != null && !chatId.isEmpty() && !chatId.equals("your_chat_id")) {
            System.out.println("✅ Chat ID is configured");
        } else {
            System.out.println("❌ Chat ID is not configured properly");
        }
    }
}

@SpringBootApplication
class TestApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TestApp.class, args);
        TelegramConfigTest test = context.getBean(TelegramConfigTest.class);
        test.testConfiguration();
        context.close();
    }
}
