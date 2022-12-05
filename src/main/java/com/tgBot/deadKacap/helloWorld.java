package com.tgBot.deadKacap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class helloWorld extends TelegramLongPollingBot {
    static boolean send = false;
    static boolean kacap = false;
    static SendMessage sm = new SendMessage();
    static DeleteMessage dm = new DeleteMessage();
    static Message message;
    static String text;
    static StringBuilder log = new StringBuilder();

    @Override
    public void onUpdateReceived(Update update) {
        message = update.hasMessage() ? update.getMessage() : update.hasEditedMessage() ? update.getEditedMessage() : null;
        if (message != null && message.hasText()
                && (message.getChat().isSuperGroupChat() || message.getChat().isGroupChat())) {
            boolean tenMinutes = message.getDate() - (System.currentTimeMillis() / 1000L) <= -600;
            text = message.getText().toLowerCase();
            log.setLength(0);
            log.append("\n").append(text).append("\n");
            int i = 0;
            log.append(setLog(++i));
            kacapWords1();
            log.append(setLog(++i));
            if (!kacap) { kacapWords2(); }
            log.append(setLog(++i));
            if (!kacap) { rusEng(); }
            log.append(setLog(++i));
            if (!send) { checkWords(); }
            log.append(setLog(++i));
            if (log.substring(log.indexOf("\n")+1).contains(" true") && message.getChat().getUserName() != null) {
                System.out.print(log);
                try {
                    Files.write(Path.of("log.txt"), log.toString().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
            if (kacap) { kacap = false; dm = new DeleteMessage(); }
            if (send) { send = false; sm = new SendMessage(); }
        }
    }

    public static boolean setText(String text) {
        sm.setText(text);
        sm.setChatId(message.getChatId());
        return true;
    }
    public static void kacapWords1() {
        for (String kacapWord : kacapWords) {
            if (text.contains(kacapWord)) {
                kacap = true;
                send = setText("повідомлення видалено через " + (kacapWord.length() <= 2 ? "букву" : "слово")
                        + " \"" + kacapWord + "\"");
                break;
            }
        }
    }
    public static void kacapWords2() {
        for (String kacapWord2 : kacapWords2) {
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
                    if (word.contains("" + rus + eng) || word.contains("" + eng + rus)) { kacap = true; break; }
                }
            }
        }
    }
    public static void checkWords() {
        String[] inputsEquals = {"/start", "/start@deadkacapbot"};
        String[] outputsEquals = {"привіт! я запущений і прямо зараз працюю!"};
        int j = 0;
        for (int i = 0; i < inputsEquals.length; i++) {
            //j += шось там;
            send = text.equals(inputsEquals[i]) ? setText(outputsEquals[j]) : send;
        }
        String[] inputsContains = {"/start", "/start@deadkacapbot", "слава Україні".toLowerCase(), "слава нації",
                "путін", "путин", "путлер", "путлєр"};
        String[] outputsContains = {"привіт! я запущений і прямо зараз працюю!", "героям слава!", "смерть кацапам!",
                "путін - хуйло! кацапи - нелюди!"};
        //j = 0;
        for (int i = 0; i < inputsContains.length; i++) {
            j += (i < 2 || i > 4 ? 0 : 1);
            send = text.contains(inputsContains[i]) ? setText(outputsContains[j]) : send;
        }
        if (send) {
            sm.setReplyMarkup(sm.getReplyMarkup());
            sm.setReplyToMessageId(message.getMessageId());
        }
    }
    public static String setLog(int num) {
        return kacap || send ? num + " " + kacap + " " + send + "\n" : "";
    }
    public static void displayWriteLog(Message message, Exception e) {
        String[] errors = {"message can't be deleted", "message to delete not found", "have no rights to send a message",
        "replied message not found"};
        String[] answers = {"повідомлення не може бути видалене", "повідомлення не знайдено", "немає прав для відправлення",
        "повідомлення для відповіді не знайдене"};
        String answer = e.getMessage();
        for (int i = 0; i < errors.length; i++) {
            if (e.getMessage().contains(errors[i])) { answer = answers[i]; }
        }
        long chatID = Math.abs(message.getChatId());
        String error = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                + answer + ": " + message.getChat().getTitle() + "; https://t.me/c/"
                + (chatID > 10_000_000_000L ? chatID - 1_000_000_000_000L : chatID)
                + "/" + message.getMessageId() + "\n";
        System.out.print(error);
        try {
            Files.write(Path.of("log.txt"), error.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    static String[] kacapWords = ("э ы ъ ё ьі " +
            "заебись ебат ебал тупой пидорас пидарас нихуя хуел еблан хуеть " +
            "привет здаров здравствуй спасибо слушай тебя работ свободн " +
            "почему тогда когда только почт пример русс росси понял далее " +
            "запрет меня добавь другой совсем понятно брос освобо согл хотел наверно мальчик девочк здрасте " +
            "надеюс вреш скольк поздр разговари нрав слуша удобн смотр общ помог ").split(" ");
    static String[] kacapWords2 = ("как кто никто некто он его она оно они их еще что што пон нипон непон кринж " +
            "какой какие каких нет однако пока если меня сегодня и иди потом дашь пиздец лет мне ищу надо мой твой " +
            "свои свой зачем нужно надо всем есть ебет сейчас ща щя щас щяс либо может любой любая че чего где везде " +
            "игра играть играю двое трое хорошо улиц улица улице пиздос пошел пошла дела дело ваще срочно " +
            "жду ждать ждешь даже ребята пожалуйста вдруг ").split(" ");
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
