package com.tgBot.deadKacap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
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
    static SendMessage sm = new SendMessage();
    static DeleteMessage dm = new DeleteMessage();
    static String text;
    static String toggle;
    static StringBuilder excludeStr = new StringBuilder();
    static StringBuilder log = new StringBuilder();

    @Override
    public void onUpdateReceived(Update update) {
        try (Scanner toggleSc = new Scanner(new FileReader("config.txt")); 
        	Scanner excludeSc = new Scanner(new FileReader("exclude.txt"))) {
        		toggle = toggleSc.hasNext() ? toggleSc.nextLine() : "";
        		while (excludeSc.hasNext()) {
        			excludeStr.append(excludeSc.nextLine()).append("\n");
        		}
        	} catch (FileNotFoundException e) {
                throw new RuntimeException(e);
        }
        message = update.hasMessage() ? update.getMessage() 
        	: update.hasEditedMessage() ? update.getEditedMessage() : null;
        if (message != null) {
            text = (message.getText() != null ? message.getText()
                    : message.getCaption() != null ? message.getCaption() : "")
                    .toLowerCase();
            enabled = !toggle.contains(message.getChatId() + "");
            if (excludeStr.toString().contains(message.getChatId() + "")) {
                int index = excludeStr.indexOf(message.getChatId() + "");
                String cropped = excludeStr.substring(index,
                        excludeStr.substring(index).contains("\n")
                                ? excludeStr.substring(index).indexOf("\n") + excludeStr.substring(0, index).length()
                                : excludeStr.length());
                String[] excl = cropped.replace(message.getChatId() + " ", "").split(" ");
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
                    && (message.getChat().isSuperGroupChat() || message.getChat().isGroupChat())) {
                tenMinutes = message.getDate() - (System.currentTimeMillis() / 1000L) <= -600;
                log.setLength(0);
                log.append("\n").append(text).append("\n");
                int i = 0;
                log.append(setLog(++i));
                kacapWords1(); log.append(setLog(++i));
                if (!kacap) { kacapWords2(); } log.append(setLog(++i));
                if (!kacap) { rusEng(); } log.append(setLog(++i));
                if (!send) { checkWords(); } log.append(setLog(++i));
                if (log.substring(log.indexOf("\n") + 1).contains(" true") 
                	&& message.getChat().getUserName() != null) {
                    System.out.print(log);
                    append("log.txt", log.toString());
                }
            }
            try {
                if (kacap) {
                    dm.setChatId(message.getChatId());
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
        excludeStr.setLength(0);
        getKacapWordsList = new ArrayList<>(Arrays.asList(kacapWordsList));
        getKacapWordsList2 = new ArrayList<>(Arrays.asList(kacapWordsList2));
    }

    public static boolean setText(String answer) {
        sm.setText(answer);
        sm.setChatId(message.getChatId());
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
    		send = setText("привіт! я запущений і прямо зараз працюю!" 
    			+ (!enabled ? "\nбота вимкнено! щоб увімкнути: /toggle" : "\nдоступні команди:\n" +
                    "/toggle - увімкнути/вимкнути бота\n/exclude - виключити непотрібні слова"));
    	} else if (equalsCommand("toggle")) {
        	try (FileWriter fw = new FileWriter("config.txt")) {
            	fw.write(enabled ? toggle + message.getChatId() : toggle.replace(message.getChatId() + "", ""));
            	send = setText("бота " + (enabled ? "вимкнено" : "увімкнено") + " в чаті! " +
            		"щоб " + (enabled ? "вимкнути" : "увімкнути") + " знову: /toggle");
        	} catch (IOException e) {
            	throw new RuntimeException(e);
        	}
        } else if (text.startsWith("/exclude ") || equalsCommand("exclude")) {
        	try (FileWriter fw = new FileWriter("exclude.txt")) {
        		if (equalsCommand("exclude")) {
        			send = setText("/exclude - команда, яка дозволяє виключати непотрібні вам слова в чаті.\n\n" +
                            "використання команди: /exclude слово1 слово2\nприклад: /exclude привет кто\n\n" +
                            "щоб видалити всі виключення: /exclude .");
        		} else {
        			if (excludeStr.toString().contains(message.getChatId() + "")) {
                        int indexID = excludeStr.indexOf(message.getChatId() + "");
                        fw.write(excludeStr.substring(0, indexID) +
                                message.getChatId() + " " + text.replace(command("exclude"), ""));
                    } else {
        				fw.write(excludeStr + (message.getChatId() + " " + text.replace(command("exclude"), "")));
                    }
                    send = setText("виключення записано!");
                    enabled = false;
        		}
        	} catch (IOException ex) {
                throw new RuntimeException(ex);
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
        final boolean[] what = {false};
        try {
            execute(new GetChatAdministrators(message.getChatId() + "")).forEach(chatMember1 -> {
                if (message.getFrom().getId().equals(chatMember1.getUser().getId())) {
                    what[0] = true;
                }
            });
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return what[0];
    }
    public static String setLog(int num) {
        return kacap || send ? num + " " + kacap + " " + send + "\n" : "";
    }
    public static void displayWriteLog(Message message, Exception e) {
        String[] errors = {"message can't be deleted", "message to delete not found",
        "have no rights to send a message", "replied message not found"};
        String[] answers = {"повідомлення не може бути видалене", "повідомлення не знайдено",
        "немає прав для відправлення", "повідомлення для відповіді не знайдене"};
        String answer = e.getMessage();
        for (int i = 0; i < errors.length; i++) {
            if (answer.contains(errors[i])) { answer = answers[i]; }
        }
        String error = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                + answer + ": " + message.getChat().getTitle() +
                (message.getChat().getUserName() != null ? "; https://t.me/" + message.getChat().getUserName()
                + "/" + message.getMessageId() : "") + "\n";
        System.out.print(error);
        append("log.txt", error);
    }
    static String[] kacapWordsList = (
    "э ы ъ ё ьі " +
    "заебись ебат ебал тупой пидорас пидарас нихуя хуел еблан хуеть привет здаров здравствуй спасибо слуша " +
    "работ свободн почему тогда когда только почт пример русс росси понял далее запрет добавь другой совсем " +
    "понятно брос освобо согл хотел наверн мальчик девочк здрасте надеюс вреш скольк поздр разговари нрав слуша " +
    "удобн смотр общ админ делать делай извини прости удали ").split(" ");
    static ArrayList<String> getKacapWordsList = new ArrayList<>(Arrays.asList(kacapWordsList));
    static String[] kacapWordsList2 = (
    "как кто никто некто он его она оно они их еще что што пон нипон непон кринж " +
    "какой какие каких нет однако пока если меня тебя сегодня и иди потом дашь пиздец лет мне ищу надо мой твой " +
    "свои свой зачем нужно надо всем есть ебет сейчас ща щя щас щяс либо может любой любая че чего где везде " +
    "игра играть играю двое трое хорошо улиц улица улице пиздос пошел пошла дела дело делаешь ваще срочно " +
    "жду ждать ждешь даже ребята пожалуйста вдруг помоги помогите помощь помог помогла хуже играеш нужен будешь " +
    "хочешь пишите умею хотя найти нашел нашла удачи ").split(" ");
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
}
