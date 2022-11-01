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

    public boolean setText(String text, SendMessage sm, Message message) {
        sm.setText(text);
        sm.setChatId(message.getChatId());
        return true;
    }
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

    @Override
    public void onUpdateReceived(Update update) {
        String[] kacapWords = {"э", "ы", "ъ", "ё", "ьі", "привет", "дела", "что", "заебись", "если", "тупой",
        "слушай", "тебя", "меня", "работ", "свободн", "ебат", "ебет", "здарова", "почему", "ебал", "когда", "только", "почт",
        "русс", "росси", "пидорас", "пидарас", "нихуя", "хуел", "пиздо", "понял", "еблан", "далее", "запрет",
        "меня", "добавь", "другой", "совсем", "понятно", "брос", "освобо", "согл", "хотел", "наверно",
        "мальчик", "девочк", "здрасте", "здравствуй", "надеюс", "вреш", "скольк", "поздр", "разговари", "нрав", "слуша"};
        String[] kacapWords2 = {"как", "кто", "никто", "некто", "он", "его", "она", "оно", "они", "их", "еще", "што",
        "пон", "нипон", "непон", "кринж", "какой", "какие", "каких", "нет", "однако", "пока",
        "сегодня", "и", "иди", "потом", "дашь", "пиздец", "лет", "мне", "ищу", "надо", "мой", "твой", "свои", "свой",
        "зачем", "нужно", "надо", "всем", "есть"};
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText() && (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat())) {
                String text = message.getText().toLowerCase();
                Scanner messc = new Scanner(text);
                SendMessage sm = new SendMessage(); DeleteMessage dm = new DeleteMessage();
                boolean kacap = false; boolean send = false;
                if (text.equals("/start") || text.equals("/start@deadkacapbot")) {
                    send = setText("привіт! я запущений і прямо зараз працюю!", sm, message);
                } else {
                    for (String kacapWord : kacapWords) {
                        if (text.contains(kacapWord)) { kacap = true; break; }
                    }
                    if (!kacap) {
                        for (String kacapWord2 : kacapWords2) {
                            Pattern pattern = Pattern.compile("\\b" + kacapWord2 + "\\b");
                            Matcher matcher = pattern.matcher(text);
                                kacap = matcher.find();
                                if (kacap) { break; }
                        }
                    }
                    if (!kacap) {
                        loop: while (messc.hasNext()) {
                            String word = messc.next();
                            for (char eng = 97; eng <= 122; eng++) {
                                for (char rus = 1072; rus <= 1103; rus++) {
                                    if (word.contains("" + eng) && word.contains("" + rus)) { kacap = true; break loop; }
                                }
                            }
                        }
                    }
                    String[] putlerWords = {"путін", "путин", "путлер", "путлєр"};
                    for (String putlerWord : putlerWords) {
                        send = text.contains(putlerWord) && setText("путін - хуйло! кацапи - нелюди!", sm, message);
                        if (send) { break; }
                    }
                    send = send || text.contains("привет")
                            && setText("привіт! я чищу повідомлення російською, тому ніяких \"привет\"", sm, message)
                            || text.contains("слава Україні".toLowerCase()) && setText("героям слава!", sm, message)
                            || text.contains("слава нації") && setText("смерть кацапам!", sm, message);
                }
                dm.setChatId(message.getChatId());
                dm.setMessageId(message.getMessageId());
                try {
                    if (kacap) { execute(dm); }
                    if (send) { execute(sm); }
                } catch (TelegramApiException e) {
                    long chatID = Math.abs(message.getChatId());
                    System.out.println("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                    + (e.getMessage().contains("message can't be deleted")
                    ? "повідомлення не може бути видалене" : e.getMessage())
                    + ": " + message.getChat().getTitle() + "; https://t.me/c/"
                    + (chatID > 10_000_000_000L ? chatID - 1_000_000_000_000L : chatID)
                    + "/" + message.getMessageId());
                }
            } else if (message.hasText()) {
                SendMessage sm = new SendMessage();
                setText("привіт! я не відповідаю в особистих повідомленнях, спробуй мене в групі." +
                        "\n" + "(боту необхідні права на видалення повідомлень)", sm, message);
                try {
                    execute(sm);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
