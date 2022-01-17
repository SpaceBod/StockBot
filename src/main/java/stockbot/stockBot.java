package stockbot;
import discord4j.common.util.Snowflake;
import io.netty.channel.ChannelId;
import org.openqa.selenium.WebDriver;
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
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.core.object.entity.channel.*;

import java.time.Instant;

public class stockBot {
    static String token = "Insert Token";

    static boolean run = false;

    static String lastNvidiaList = "Type !start  to run";
    static StringBuilder nvidiaList = new StringBuilder();
    static String nvidiaFieldName = "**NVIDIA**";

    static String lastAmdList = "Type !start  to run";
    static StringBuilder amdList = new StringBuilder();
    static String amdFieldName = "**AMD**";

    static boolean nvidiaInStock = false;
    static boolean amdInStock = false;

    static long nvidiaCoolDown = 0;
    static long amdCoolDown = 0;
    static long nvidiaTime = Instant.now().getEpochSecond();
    static long amdTime = Instant.now().getEpochSecond();

    public static int parseStringToInt(String s){
        s = s.replaceAll(",", ""); //remove commas
        s = s.replaceAll("£", "");
        return (int)Math.round(Double.parseDouble(s)); //return rounded double cast to int
    }




    public static void main(String[] args) {

        WebDriverManager.chromedriver().driverVersion("96.0.4664.45").setup();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        stockBot bot = new stockBot();

        GatewayDiscordClient client = DiscordClientBuilder.create(token).build().login().block();
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });

        Thread botService = new Thread() {
            public void run() {
                client.getEventDispatcher().on(MessageCreateEvent.class)
                        .map(MessageCreateEvent::getMessage)
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .filter(message -> message.getContent().equalsIgnoreCase("!exit"))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(">>> Terminating Bot"))
                        .doOnNext(command -> {
                            System.out.println("-- Terminating Bot --");
                            QuitAll();
                        })
                        .subscribe();
                client.getEventDispatcher().on(MessageCreateEvent.class)
                        .map(MessageCreateEvent::getMessage)
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .filter(message -> message.getContent().equalsIgnoreCase("!start"))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(">>> ▶️"))
                        .doOnNext(command -> {
                            System.out.println("[USER IN] !START");
                            run = true;
                        })
                        .subscribe();
                client.getEventDispatcher().on(MessageCreateEvent.class)
                        .map(MessageCreateEvent::getMessage)
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .filter(message -> message.getContent().equalsIgnoreCase("!stop"))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(">>> ⏸️"))
                        .doOnNext(command -> {
                            System.out.println("[USER IN] !STOP");
                            run = false;
                        })
                        .subscribe();
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
                                .footer("SpaceBod#0001 - All Rights Reserved", "https://cdn.discordapp.com/avatars/656963296188039176/a_216e17d73bee63318278306f66dfae79.png")
                                .build()))
                        .flatMap(Message::getChannel)
                        .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .title("AMD Store")
                                .description("Will notify when queue opens...")
                                .url("https://www.amd.com/en/direct-buy/gb")
                                .addField(amdFieldName, "```" + lastAmdList + "```", true)
                                .timestamp(Instant.now())
                                .footer("SpaceBod#0001 - All Rights Reserved", "https://cdn.discordapp.com/avatars/656963296188039176/a_216e17d73bee63318278306f66dfae79.png")
                                .build()))
                        .doOnNext(command -> {
                            System.out.println("[USER IN] !GPU");
                        })
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
                    options.addArguments("--disable-gpu");
                    options.addArguments("--no-sandbox");
                    options.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36");
                    String nvidiaURL = "https://store.nvidia.com/en-gb/geforce/store/gpu/?page=1&limit=9&locale=en-gb&gpu=RTX%203090,RTX%203080,RTX%203080%20Ti,RTX%203070%20Ti,RTX%203070,RTX%203060%20Ti&manufacturer=NVIDIA&category=GPU&category_filter=GPU~0,LAPTOP~0,STUDIO-LAPTOP~0,NVLINKS~0";
                    String amdURL = "https://www.amd.com/en/direct-buy/gb";
                    String amdLineURL = "inline.amd.com";
                    ChromeDriver driver = new ChromeDriver(options);

                    while (run) {
                        try {
                            // Open Nvidia Store Page
                            driver.get(nvidiaURL);
                            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
                            Thread.sleep(1000);
                            // Gather Nvidia Element Text
                            List<WebElement> nvidiaTitle = driver.findElements(By.className("name"));
                            List<WebElement> nvidiaStock = driver.findElements(By.className("buy"));
                            List<WebElement> nvidiaPrice = driver.findElements(By.className("price"));
                            //System.out.println("Number of NVIDIA items:" + nvidiaTitle.size());

                            nvidiaList.append(String.format("%-16s%-12s%-8s%n", "Item", "Price", "Stock"));
                            nvidiaList.append("\n");
                            for (int i = 0; i < nvidiaTitle.size(); i++) {
                                String nvidiaTitleString = (nvidiaTitle.get(i).getText().replaceAll(".*GEFORCE ", ""));
                                String nvidiaPriceString = ("£" + parseStringToInt((nvidiaPrice.get(i).getText())));
                                if (nvidiaStock.get(i).getText().equals("OUT OF STOCK")) {
                                    String nvidiaTemp = String.format("%-16s%-12s%-8s%n", nvidiaTitleString, nvidiaPriceString, "❌");
                                    nvidiaList.append(nvidiaTemp);
                                } else {
                                    String nvidiaTemp = String.format("%-16s%-12s%-8s%n", nvidiaTitleString, nvidiaPriceString, "✅");
                                    nvidiaList.append(nvidiaTemp);
                                    nvidiaInStock = true;
                                }
                            }

                            // Open AMD Store Page
                            driver.get(amdURL);
                            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

                            // Gather AMD Element Text
                            if (!driver.findElements(By.className("shop-title")).isEmpty()) {
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
                                    } else {
                                        String amdTemp = String.format("%-16s%-12s%-8s%n", amdTitleString, amdPriceString, "✅");
                                        amdList.append(amdTemp);
                                        if (driver.getCurrentUrl().contains(amdLineURL)) {
                                            amdInStock = true;
                                        }
                                    }
                                }
                                System.out.println(dtf.format(LocalDateTime.now()) + "\nN-" + nvidiaTitle.size() + " A-" + amdTitle.size());

                            }
                            else {
                                if (driver.getCurrentUrl().contains(amdLineURL)) {
                                    amdInStock = true;
                                }
                                String amdTemp = String.format("%-16s%-12s%-8s%n", "QUEUE", "OPEN", "✅");
                                amdList.append(amdTemp);
                                System.out.println(dtf.format(LocalDateTime.now()) + "\nN-" + nvidiaTitle.size() + " A-0");
                            }

                            // Time Stamp
                            LocalDateTime now = LocalDateTime.now();
                            nvidiaFieldName = ("**NVIDIA** " + "*@" + dtf.format(now) + "*");
                            amdFieldName = ("**AMD** " + "*@" + dtf.format(now) + "*");

                            lastNvidiaList = nvidiaList.toString();
                            lastAmdList = amdList.toString();
                            nvidiaList.setLength(0);
                            amdList.setLength(0);

                            // Sends Stock Alert for Nvidia
                            long timeNow = Instant.now().getEpochSecond();
                            if (nvidiaInStock && (timeNow - nvidiaTime > nvidiaCoolDown)) {
                                client.getChannelById(Snowflake.of("928057536165347339"))
                                        .ofType(MessageChannel.class)
                                        .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                                                .color(Color.GREEN)
                                                .title("NVIDIA - GPU AVAILABLE")
                                                .description("*Last checked" + " @" + dtf.format(now) + "*")
                                                .thumbnail("https://www.nvidia.com/content/dam/en-zz/Solutions/about-nvidia/logo-and-brand/02-nvidia-logo-color-grn-500x200-4c25-p@2x.png")
                                                .url(nvidiaURL)
                                                .timestamp(Instant.now())
                                                .footer("SpaceBod#0001 - All Rights Reserved", "https://cdn.discordapp.com/avatars/656963296188039176/a_216e17d73bee63318278306f66dfae79.png")
                                                .build()))
                                        .subscribe();
                                client.getChannelById(Snowflake.of("928057536165347339")).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage("@everyone")).subscribe();
                                client.getChannelById(Snowflake.of("928056739482435614")).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage(">>> " + nvidiaFieldName + "\n" + "```" + lastNvidiaList + "```")).subscribe();
                                nvidiaInStock = false;
                                nvidiaTime = Instant.now().getEpochSecond();
                                nvidiaCoolDown = 60;
                            }

                            // Sends Stock Alert for AMD
                            if (amdInStock && (timeNow - amdTime > amdCoolDown)) {
                                client.getChannelById(Snowflake.of("928057536165347339"))
                                        .ofType(MessageChannel.class)
                                        .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                                                .color(Color.RED)
                                                .title("AMD - GPU QUEUE OPEN")
                                                .description("*Last checked" + " @" + dtf.format(now) + "*")
                                                .thumbnail("https://www.wallpapertip.com/wmimgs/56-564463_amd-white-logo-png.png")
                                                .url(amdURL)
                                                .timestamp(Instant.now())
                                                .footer("SpaceBod#0001 - All Rights Reserved", "https://cdn.discordapp.com/avatars/656963296188039176/a_216e17d73bee63318278306f66dfae79.png")
                                                .build()))
                                        .subscribe();
                                client.getChannelById(Snowflake.of("928057536165347339")).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage("@everyone")).subscribe();
                                client.getChannelById(Snowflake.of("928056739482435614")).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage(">>> " + amdFieldName + "\n" + "```" + lastAmdList + "```")).subscribe();
                                amdInStock = false;
                                amdTime = Instant.now().getEpochSecond();
                                amdCoolDown = 600;
                            }
                            Thread.sleep(2500);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
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

    public static void QuitAll() {
        System.out.println("Exiting...");
        System.exit(0);
    }
}

