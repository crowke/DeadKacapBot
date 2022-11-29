package com.tgBot.deadKacap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
    static Message message;
    static String text;

    @Override
    public void onUpdateReceived(Update update) {
        message = update.hasMessage() ? update.getMessage() : update.hasEditedMessage() ? update.getEditedMessage() : null;
        if (message != null && message.hasText()
                && (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat())) {
            boolean tenMinutes = message.getDate() - (System.currentTimeMillis() / 1000L) <= -600;
            text = message.getText().toLowerCase();
            DeleteMessage dm = new DeleteMessage();
            kacapWords1();
            if (!kacap) { kacapWords2(); }
            //if (!kacap) { rusEng(); }
            checkWords();
            try {
                if (kacap) {
                    dm.setChatId(message.getChatId());
                    dm.setMessageId(message.getMessageId());
                    execute(dm);
                    kacap = false;
                }
                if (send && !tenMinutes) { execute(sm); send = false; }
            } catch (TelegramApiException e) {
                displayLog(message, e);
            }
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
        String[] inputs = {"/start", "/start@deadkacapbot", "слава Україні".toLowerCase(), "слава нації",
                "путін", "путин", "путлер", "путлєр"};
        String[] outputs = {"привіт! я запущений і прямо зараз працюю!", "героям слава!", "смерть кацапам!",
                "путін - хуйло! кацапи - нелюди!"};
        int j = 0;
        for (int i = 0; i < inputs.length; i++) {
            j += (i < 2 || i > 4 ? 0 : 1);
            send = text.contains(inputs[i]) ? setText(outputs[j]) : send;
        }
    }
    public static void displayLog(Message message, Exception e) {
        long chatID = Math.abs(message.getChatId());
        System.out.println("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                + (e.getMessage().contains("message can't be deleted")
                ? "повідомлення не може бути видалене" : e.getMessage())
                + ": " + message.getChat().getTitle() + "; https://t.me/c/"
                + (chatID > 10_000_000_000L ? chatID - 1_000_000_000_000L : chatID)
                + "/" + message.getMessageId());
    }
    static String[] kacapWords = {"э", "ы", "ъ", "ё", "ьі", "привет", "дела", "что", "заебись", "тупой", "спасибо",
            "слушай", "тебя", "работ", "свободн", "ебат", "здарова", "почему", "ебал", "когда", "только", "почт",
            "пример", "русс", "росси", "пидорас", "пидарас", "нихуя", "хуел", "пиздо", "понял", "еблан", "далее",
            "запрет", "меня", "добавь", "другой", "совсем", "понятно", "брос", "освобо", "согл", "хотел", "наверно",
            "хуеть", "мальчик", "девочк", "здрасте", "здравствуй", "надеюс", "вреш", "скольк", "поздр",
            "разговари", "нрав", "слуша", "двое", "трое", "сейчас"};
    static String[] kacapWords2 = {"как", "кто", "никто", "некто", "он", "его", "она", "оно", "они", "их", "еще", "што",
            "пон", "нипон", "непон", "кринж", "какой", "какие", "каких", "нет", "однако", "пока", "если", "меня",
            "сегодня", "и", "иди", "потом", "дашь", "пиздец", "лет", "мне", "ищу", "надо", "мой", "твой", "свои", "свой",
            "зачем", "нужно", "надо", "всем", "есть", "ебет", "ща", "щя", "щас", "щяс", "либо", "может", "любой", "любая",
            "че", "чего", "где", "игра", "играть", "играю"};
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
