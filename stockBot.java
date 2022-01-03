package stockbot;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.lang.invoke.ConstantCallSite;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import org.openqa.selenium.Keys;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.time.Instant;

public class stockBot {

    static boolean run = false;

    static String lastNvidiaList = "Type !start  to run";
    static StringBuilder nvidiaList = new StringBuilder();
    static String nvidiaFieldName = "**NVIDIA**";

    static String lastAmdList = "Type !start  to run";
    static StringBuilder amdList = new StringBuilder();
    static String amdFieldName = "**AMD**";

    static boolean inStock = false;

    public static int parseStringToInt(String s){
        s = s.replaceAll(",", ""); //remove commas
        s = s.replaceAll("£", "");
        return (int)Math.round(Double.parseDouble(s)); //return rounded double cast to int
    }

    public static void main(String[] args) {

        WebDriverManager.chromedriver().setup();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        stockBot bot = new stockBot();

        Thread botService = new Thread() {
            public void run() {
                GatewayDiscordClient client = DiscordClientBuilder.create(-- DISCORD TOKEN --).build().login().block();
                client.getEventDispatcher().on(ReadyEvent.class)
                        .subscribe(event -> {
                            User self = event.getSelf();
                            System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                        });
                client.getEventDispatcher().on(MessageCreateEvent.class)
                        .map(MessageCreateEvent::getMessage)
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .filter(message -> message.getContent().equalsIgnoreCase("!start"))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(">>> ▶️"))
                        .subscribe(event -> {
                            run = true;
                            System.out.println("[USER IN] !START");
                        });
                client.getEventDispatcher().on(MessageCreateEvent.class)
                        .map(MessageCreateEvent::getMessage)
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .filter(message -> message.getContent().equalsIgnoreCase("!stop"))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(">>> ⏸️"))
                        .subscribe(event -> {
                            run = false;
                            System.out.println("[USER IN] !STOP");
                        });

                client.getEventDispatcher().on(MessageCreateEvent.class)
                        .map(MessageCreateEvent::getMessage)
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .filter(message -> message.getContent().equalsIgnoreCase("!gpu"))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                                .color(Color.GREEN)
                                .title("NVIDIA Store")
                                .description("Will notify when in stock...")
                                .url("https://store.nvidia.com/en-gb/geforce/store/gpu/?page=1&limit=9&locale=en-gb&gpu=RTX%203090,RTX%203080,RTX%203080%20Ti,RTX%203070%20Ti,RTX%203070,RTX%203060%20Ti&manufacturer=NVIDIA&category=GPU&category_filter=GPU~0,LAPTOP~0,STUDIO-LAPTOP~0,NVLINKS~0")
                                .addField(nvidiaFieldName, "```" + lastNvidiaList + "```", true)
                                .timestamp(Instant.now())
                                .footer("SpaceBod#0001 - All Rights Reserved", "https://i.imgur.com/F9BhEoz.png")
                                .build()))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .title("AMD Store")
                                .description("Will notify when in stock...")
                                .url("https://www.amd.com/en/direct-buy/gb")
                                .addField(amdFieldName, "```" + lastAmdList + "```", true)
                                .timestamp(Instant.now())
                                .footer("SpaceBod#0001 - All Rights Reserved", "https://i.imgur.com/F9BhEoz.png")
                                .build()))
                        .subscribe();

                client.onDisconnect().block();
            }
        };
        botService.start();
        while (true) {
            if (run) {
                System.out.println("running");
                try {
                    ChromeOptions options = new ChromeOptions();
                    options.addArguments("--headless");
                    options.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
                    String nvidiaURL = "https://store.nvidia.com/en-gb/geforce/store/gpu/?page=1&limit=9&locale=en-gb&gpu=RTX%203090,RTX%203080,RTX%203080%20Ti,RTX%203070%20Ti,RTX%203070,RTX%203060%20Ti&manufacturer=NVIDIA&category=GPU&category_filter=GPU~0,LAPTOP~0,STUDIO-LAPTOP~0,NVLINKS~0";
                    String amdURL = "https://www.amd.com/en/direct-buy/gb";
                    ChromeDriver driver = new ChromeDriver(options);

                    while (run) {
                        // Open Nvidia Store Page
                        driver.get(nvidiaURL);
                        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));

                        // Gather Nvidia Element Text
                        List<WebElement> nvidiaTitle = driver.findElements(By.className("name"));
                        List<WebElement> nvidiaStock = driver.findElements(By.className("buy"));
                        List<WebElement> nvidiaPrice = driver.findElements(By.className("price"));
                        System.out.println("Number of NVIDIA items:" + nvidiaTitle.size());

                        nvidiaList.append(String.format("%-16s%-12s%-8s%n", "Item", "Price", "Stock"));
                        nvidiaList.append("\n");
                        for (int i = 0; i < nvidiaTitle.size(); i++) {
                            String nvidiaTitleString = (nvidiaTitle.get(i).getText().replaceAll(".*GEFORCE ", ""));
                            String nvidiaPriceString = ("£" + parseStringToInt((nvidiaPrice.get(i).getText())));
                            if (nvidiaStock.get(i).getText().equals("OUT OF STOCK")) {
                                String nvidiaTemp = String.format("%-16s%-12s%-8s%n", nvidiaTitleString, nvidiaPriceString, "❌");
                                nvidiaList.append(nvidiaTemp);
                            }
                            else {
                                String nvidiaTemp = String.format("%-16s%-12s%-8s%n", nvidiaTitleString, nvidiaPriceString, "✅");
                                nvidiaList.append(nvidiaTemp);
                                inStock = true;
                            }
                        }

                        // Open AMD Store Page
                        driver.get(amdURL);
                        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));

                        // Gather AMD Element Text
                        List<WebElement> amdTitle = driver.findElements(By.className("shop-title"));
                        List<WebElement> amdStock = driver.findElements(By.className("shop-links"));
                        List<WebElement> amdPrice = driver.findElements(By.className("shop-price"));
                        amdTitle.remove(0);
                        amdTitle.subList(5, amdTitle.size()).clear();
                        amdStock.remove(0);
                        amdStock.subList(5, amdTitle.size()).clear();
                        amdPrice.remove(0);
                        amdPrice.subList(5, amdTitle.size()).clear();

                        amdList.append(String.format("%-16s%-12s%-8s%n", "Item", "Price", "Stock"));
                        amdList.append("\n");

                        for (int i = 0; i < amdTitle.size(); i++) {
                            String amdTitleString = amdTitle.get(i).getText().replaceAll(".*Radeon™ ", "").split("Graphics")[0].split("idnight")[0];
                            String amdPriceString = ("£" + parseStringToInt((amdPrice.get(i).getText())));
                            if (amdStock.get(i).getText().equals("Out of Stock")) {
                                String amdTemp = String.format("%-16s%-12s%-8s%n", amdTitleString, amdPriceString, "❌");
                                amdList.append(amdTemp);
                            }
                            else {
                                String amdTemp = String.format("%-16s%-12s%-8s%n", amdTitleString, amdPriceString, "✅");
                                amdList.append(amdTemp);
                            }
                        }

                        // Time Stamp
                        LocalDateTime now = LocalDateTime.now();
                        nvidiaFieldName =("**NVIDIA** " + "*@" + dtf.format(now) + "*");
                        amdFieldName =("**AMD** " + "*@" + dtf.format(now) + "*");
                        //amdTitleString.append("@ " + dtf.format(now));

                        lastNvidiaList = nvidiaList.toString();
                        System.out.println(lastNvidiaList);
                        nvidiaList.setLength(0);
                        lastAmdList = amdList.toString();
                        System.out.println(lastNvidiaList);
                        amdList.setLength(0);

                        try {
                            Thread.sleep(4000);
                        }
                        catch (Exception e) {
                        }
                    }
                    driver.quit();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            }
            catch (Exception e) {
            }
        }
    }
}

