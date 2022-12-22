package com.tgBot.deadKacap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class helloWorld extends TelegramLongPollingBot {
    static Message message;
    static boolean send = false;
    static boolean kacap = false;
    static boolean enabled = true;
    static boolean isForward = false;
    static boolean forwardEnabled = true;
    static SendMessage sm = new SendMessage();
    static DeleteMessage dm = new DeleteMessage();
    static User bot;
    static long botID;
    static Chat chat;
    static String id;
    static String text;
    static String toggle;
    static String forward;
    static StringBuilder exclude = new StringBuilder();
    static StringBuilder log = new StringBuilder();

    @Override
    public void onUpdateReceived(Update update) {
        try (Scanner toggleSc = new Scanner(new FileReader("config.txt")); 
        	Scanner excludeSc = new Scanner(new FileReader("exclude.txt"));
            Scanner forwardSc = new Scanner(new FileReader("forward.txt"))) {
        		toggle = toggleSc.hasNext() ? toggleSc.nextLine() : "";
                forward = forwardSc.hasNext() ? forwardSc.nextLine() : "";
        		while (excludeSc.hasNext()) { exclude.append(excludeSc.nextLine()).append("\n"); }
        	} catch (FileNotFoundException e) {
                throw new RuntimeException(e);
        }
        message = update.hasMessage() ? update.getMessage() 
        	: update.hasEditedMessage() ? update.getEditedMessage() : null;
        if (message != null) {
            bot = getBot();
            botID = bot.getId();
            text = (message.getText() != null ? message.getText()
                    : message.getCaption() != null ? message.getCaption() : "")
                    .toLowerCase();
            chat = message.getChat();
            id = chat.getId() + "";
            enabled = !toggle.contains(id);
            isForward = message.getForwardDate() != null;
            forwardEnabled = !forward.contains(id);
            if (exclude.toString().contains(id)) {
                int index = exclude.indexOf(id);
                String cropped = exclude.substring(index,
                        exclude.substring(index).contains("\n")
                                ? exclude.substring(index).indexOf("\n") + exclude.substring(0, index).length()
                                : exclude.length());
                String[] excl = cropped.replace(id + " ", "").split(" ");
                for (String exc : excl) {
                    getKacapWordsList.removeIf(exc::equals);
                    getKacapWordsList.removeIf(exc::contains);
                    getKacapWordsList2.removeIf(exc::equals);
                    getKacapWordsList2.removeIf(exc::contains);
                }
            }
            if (text.startsWith("/") && isAdmin()) { setCommands(); }
            boolean tenMinutes = false;
            if (enabled && !Objects.equals(text, "")
                    && (chat.isSuperGroupChat() || chat.isGroupChat())) {
                tenMinutes = message.getDate() - (System.currentTimeMillis() / 1000L) <= -600;
                log.setLength(0);
                log.append("\n").append(text).append("\n");
                int i = 0;
                appendLog(++i);
                if (forwardEnabled || !isForward) {
                    if (canBotDelete()) {
                        kacapWords1(); appendLog(++i);
                        if (!kacap) { kacapWords2(); } appendLog(++i);
                        if (!kacap) { rusEng(); } appendLog(++i);
                    } else if (!send) {
                        send = setText("помилка: не маю прав на видалення повідомлення!\n" +
                                "надайте мені права на видалення або вимкніть мене: /toggle");
                    }
                    if (!send) { checkWords(); } appendLog(++i);
                }
                if (log.substring(log.indexOf("\n") + 1).contains(" true") && chat.getUserName() != null) {
                    System.out.print(log);
                    append("log.txt", log.toString());
                }
            }
            try {
                if (kacap) {
                    dm.setChatId(id);
                    dm.setMessageId(message.getMessageId());
                    execute(dm);
                }
                if (send && !tenMinutes) { execute(sm); }
            } catch (TelegramApiException e) {
                displayWriteLog(message, e);
            }
        }
        kacap = false;
        send = false;
        dm = new DeleteMessage();
        sm = new SendMessage();
        exclude.setLength(0);
        getKacapWordsList = new ArrayList<>(Arrays.asList(kacapWordsList));
        getKacapWordsList2 = new ArrayList<>(Arrays.asList(kacapWordsList2));
    }

    public static boolean setText(String answer) {
        sm.setText(answer);
        sm.setChatId(id);
        return true;
    }
    public static void append(String file, String input) {
        try {
            Files.write(Path.of(file), input.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void setCommands() {
        if (equalsCommand("start")) {
            send = setText("привіт! я запущений і прямо зараз працюю!\n"
                    + (!enabled ? "бота вимкнено! щоб увімкнути: /toggle"
                    : "доступні команди:\n" +
                    "/toggle - увімкнути/вимкнути бота\n" +
                    "/exclude - виключити непотрібні слова\n" +
                    "/forward - увімкнути/вимкнути видалення пересланих повідомлень\n"));
        } else {
            try (FileWriter fw = new FileWriter(
                    (equalsCommand("toggle") ? "config"
                    : text.startsWith("/exclude ") || equalsCommand("exclude") ? "exclude"
                    : equalsCommand("forward") ? "forward" : "")
                            + ".txt")) {
                if (equalsCommand("toggle")) {
                    fw.write(enabled ? toggle + id : toggle.replace(id, ""));
                    send = setText("бота " + (enabled ? "вимкнено" : "увімкнено") + " в чаті! " +
                            "щоб " + (enabled ? "увімкнути" : "вимкнути") + " знову: /toggle");
                } else if (text.startsWith("/exclude ") || equalsCommand("exclude")) {
                    if (equalsCommand("exclude")) {
                        send = setText("/exclude - команда, яка дозволяє виключати непотрібні вам слова в чаті.\n\n" +
                                "використання команди: /exclude слово1 слово2\nприклад: /exclude привет кто\n\n" +
                                "щоб видалити всі виключення:\n/exclude .");
                    } else {
                        if (exclude.toString().contains(id)) {
                            int indexID = exclude.indexOf(id);
                            fw.write(exclude.substring(0, indexID) + id + " " + text.replace(command("exclude"), "")
                                    + exclude.substring(indexID + exclude.substring(indexID).indexOf("\n")));
                            //exclude.substring(indexID).indexOf("\n") рахує довжину рядка зі вказаним id,
                            //тому функція продовжує запис, ігноруючи цей рядок
                        } else {
                            fw.write(exclude + id + " " + text.replace(command("exclude"), ""));
                        }
                        send = setText("виключення записано!");
                        enabled = false;
                    }
                } else if (equalsCommand("forward")) {
                    fw.write(forward.contains(id) ? forward.replace(id, "") : forward + id);
                    send = setText("видалення пересилань " + (forwardEnabled ? "вимкнено" : "увімкнено") + " в чаті! " +
                            "щоб " + (forwardEnabled ? "увімкнути" : "вимкнути") + " знову: /forward");
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static String command(String cmd) {
        return "/" + cmd + (text.contains("@deadkacapbot") ? "@deadkacapbot " : " ");
    }
    public static boolean equalsCommand(String command) {
    	return text.equals("/" + command) || text.equals("/" + command + "@deadkacapbot");
    }
    public static void kacapWords1() {
        for (String kacapWord : getKacapWordsList) {
            if (text.contains(kacapWord)) {
                kacap = true;
                send = setText("повідомлення видалено через " + (kacapWord.length() <= 2 ? "букву" : "слово")
                        + " \"" + kacapWord + "\"");
                break;
            }
        }
    }
    public static void kacapWords2() {
        for (String kacapWord2 : getKacapWordsList2) {
            Pattern pattern = Pattern.compile("\\b" + kacapWord2 + "\\b");
            Matcher matcher = pattern.matcher(text);
            kacap = matcher.find();
            if (kacap) { send = setText("повідомлення видалено через слово \"" + kacapWord2 + "\""); break; }
        }
    }
    public static void rusEng() {
        Scanner mesSc = new Scanner(text);
        while (mesSc.hasNext()) {
            String word = mesSc.next();
            for (char eng = 97; eng <= 122; eng++) {
                for (char rus = 1072; rus <= 1103; rus++) {
                    if (word.contains("" + rus + eng) || word.contains("" + eng + rus)) {
                        kacap = true;
                        send = setText("повідомлення видалено через заміну в слові кирилиці на англійські букви");
                        break;
                    }
                }
            }
        }
    }
    public static void checkWords() {
        int j = 0;
        String[] inputsContains = {"слава Україні".toLowerCase(), "слава нації",
        "путін", "путин", "путлер", "путлєр"};
        String[] outputsContains = {"героям слава!", "смерть кацапам!", "путін - хуйло! кацапи - нелюди!"};
        for (int i = 0; i < inputsContains.length; i++) {
            send = text.contains(inputsContains[i]) ? setText(outputsContains[j]) : send;
            j += i > 1 ? 0 : 1;
        }
        if (send) {
            sm.setReplyMarkup(sm.getReplyMarkup());
            sm.setReplyToMessageId(message.getMessageId());
        }
    }
    public boolean isAdmin() {
        final boolean[] admin = {false};
        try {
            execute(new GetChatAdministrators(id)).forEach(chatMember1 -> {
                if (message.getFrom().getId().equals(chatMember1.getUser().getId())) { admin[0] = true; }
            });
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return admin[0];
    }
    public boolean canBotDelete() {
        final boolean[] canDelete = {false};
        try {
            execute(new GetChatAdministrators(id)).forEach(chatMember -> {
                if (chatMember.getUser().getId().equals(botID)
                        && chatMember.toString().contains("canDeleteMessages=true")) { canDelete[0] = true; }
            });
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return canDelete[0];
    }
    public static void appendLog(int num) { log.append(kacap || send ? num + " " + kacap + " " + send + "\n" : ""); }
    public static void displayWriteLog(Message message, Exception e) {
        String answer = e.getMessage();
        boolean ignore = answer.contains("have no rights to send a message");
        String[] errors = {"message can't be deleted", "message to delete not found", "replied message not found"};
        String[] answers = {"повідомлення не може бути видалене", "повідомлення не знайдено",
                "повідомлення для відповіді не знайдене"};
        for (int i = 0; i < errors.length; i++) {
            if (answer.contains(errors[i])) { answer = answers[i]; }
        }
        String error = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                + answer + ": " + chat.getTitle() +
                (chat.getUserName() != null ? "; https://t.me/" + chat.getUserName()
                + "/" + message.getMessageId() : "") + "\n";
        System.out.print(error);
        if (!ignore) { append("log.txt", error); }
    }
    static String[] kacapWordsList = (
    "э ы ъ ё ьі " +
    "заебись ебат ебал тупой пидорас пидарас нихуя хуел еблан хуеть привет здаров здравствуй спасибо слуша " +
    "работ свободн почему тогда когда только почт пример русс росси понял далее запрет добавь другой совсем " +
    "понятно брос освобо согл хотел наверн мальчик девочк здрасте надеюс вреш скольк поздр разговари нрав слуша " +
    "удобн смотр общ админ делать делай извини удали ").split(" ");
    static ArrayList<String> getKacapWordsList = new ArrayList<>(Arrays.asList(kacapWordsList));
    static String[] kacapWordsList2 = (
    "как кто никто некто он его она оно они их еще что што пон поняв нипон непон кринж " +
    "какой какие каких нет однако пока если меня тебя сегодня и иди потом дашь пиздец лет мне ищу надо мой твой " +
    "свои свой зачем нужно надо всем есть ебет сейчас ща щя щас щяс либо может любой любая че чего где везде " +
    "игра играть играю двое трое хорошо улиц улица улице пиздос пошел пошла дела дело делаешь ваще срочно " +
    "жду ждать ждешь даже ребята пожалуйста вдруг помоги помогите помощь помог помогла хуже играеш нужен будешь " +
    "хочешь пишите умею хотя найти нашел нашла удачи нету жив живая живой год года лет прости простите нечто ничто " +
    "такое такой такие такая").split(" ");
    static ArrayList<String> getKacapWordsList2 = new ArrayList<>(Arrays.asList(kacapWordsList2));
    @Value("${telegram.bot.username}")
    private String username;
    @Value("${telegram.bot.token}")
    private String token;
    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
    public User getBot() {
        User bot;
        try {
            bot = execute(new GetMe());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return bot;
    }
}
