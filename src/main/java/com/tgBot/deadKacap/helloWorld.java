package com.tgBot.deadKacap;

import com.ibm.icu.text.Transliterator;
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
import java.util.regex.Pattern;

@Component
public class helloWorld extends TelegramLongPollingBot {
    Message message;
    boolean send;
    boolean kacap;
    boolean enabled = true;
    boolean isForward = false;
    boolean forwardEnabled = true;
    boolean tenMinutes = false;
    SendMessage sm;
    DeleteMessage dm;
    User bot;
    long botID;
    Chat chat;
    String chatUsername;
    String id;
    int messageId;
    String text;
    String toggle;
    String forward;
    StringBuilder exclude = new StringBuilder();
    StringBuilder log = new StringBuilder();

    @Override
    public void onUpdateReceived(Update update) {
        //kacapWordsListTranslit();
        bot = getBot();
        botID = bot.getId();
        kacap = false;
        send = false;
        dm = new DeleteMessage();
        sm = new SendMessage();
        exclude.setLength(0);
        getKacapWordsList = new ArrayList<>(Arrays.asList(kacapWordsList));
        getKacapWordsList2 = new ArrayList<>(Arrays.asList(kacapWordsList2));
        log.setLength(0);

        try (Scanner toggleSc = new Scanner(new FileReader("toggle.txt"));
        	Scanner excludeSc = new Scanner(new FileReader("exclude.txt"));
            Scanner forwardSc = new Scanner(new FileReader("forward.txt"))) {
        		toggle = toggleSc.hasNext() ? toggleSc.nextLine() : "";
                forward = forwardSc.hasNext() ? forwardSc.nextLine() : "";
        		while (excludeSc.hasNext()) {
                    exclude.append(excludeSc.nextLine()).append("\n");
                }
        	} catch (FileNotFoundException e) {
                throw new RuntimeException(e);
        }

        message = update.hasMessage() ? update.getMessage() 
        	: update.hasEditedMessage() ? update.getEditedMessage() : null;
        if (message != null) {
            System.out.println(message.getFrom());
            chat = message.getChat();
            chatUsername = chat.getUserName();
            id = chat.getId() + "";
            messageId = message.getMessageId();
            enabled = !toggle.contains(id);
            isForward = message.getForwardDate() != null;
            forwardEnabled = !forward.contains(id);
            text = (message.getText() != null ? message.getText()
                    : message.getCaption() != null ? message.getCaption() : "")
                    .toLowerCase();
            if (exclude.toString().contains(id)) {
                trimWordList();
            }
            if (text.startsWith("/")) {
                setCommands();
            }
            if (enabled && !Objects.equals(text, "") && (chat.isSuperGroupChat() || chat.isGroupChat())) {
                tenMinutes = message.getDate() - (System.currentTimeMillis() / 1000L) <= -600;
                log.append("\n").append(text).append("\n");
                int i = 0;
                appendLog(++i);
                if (!isForward || forwardEnabled) {
                    kacapWords1(); appendLog(++i);
                    if (!kacap) { kacapWords2(); } appendLog(++i);
                    if (!kacap) { rusEng(); } appendLog(++i);
                    if (!send) { checkWords(); } appendLog(++i);
                }
                if (!canBotDelete() && kacap) {
                    send = setText("помилка: не маю прав на видалення повідомлення!\n" +
                            "надайте мені права на видалення або вимкніть мене: /toggle");
                    kacap = false;
                }
                if (log.substring(log.indexOf("\n") + 1).contains(" true") && chatUsername != null) {
                    System.out.print(log);
                    append("log.txt", log.toString());
                }
            }
            try {
                if (kacap) {
                    dm.setChatId(id);
                    dm.setMessageId(messageId);
                    execute(dm);
                }
                if (send && !tenMinutes) {
                    execute(sm);
                }
            } catch (TelegramApiException e) {
                displayWriteLog(e);
            }
        }
    }

    public boolean setText(String answer) {
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
    public void setCommands() {
        if (equalsCommand("start")) {
            send = setText("привіт! я запущений і прямо зараз працюю!\n"
                    + (!enabled ? "бота вимкнено! щоб увімкнути: /toggle"
                    : "доступні команди:\n" +
                    "/toggle - увімкнути/вимкнути бота\n" +
                    "/exclude - виключити непотрібні слова\n" +
                    "/forward - увімкнути/вимкнути видалення пересланих повідомлень\n"));
        } else if (isAdmin()) {
            int indexID = exclude.indexOf(id);
            boolean eqToggle = equalsCommand("toggle");
            boolean eqExclude = equalsCommand("exclude");
            boolean stExclude = text.startsWith("/exclude ");
            boolean eqForward = equalsCommand("forward");
            String same = eqToggle ? toggle : stExclude ? exclude.toString() : eqForward ? forward : "";
            try (FileWriter fw = new FileWriter((
                 eqToggle ? "toggle"
                 : stExclude ? "exclude"
                 : eqForward ? "forward"
                 : "")
                 + ".txt")) {

                fw.write(
                    eqToggle && enabled
                    ? toggle + id

                    : eqToggle
                    ? toggle.replace(id, "")

                    : stExclude && exclude.toString().contains(id)
                    ? exclude.substring(0, indexID) + id + " " + text.replace(command("exclude"), "")
                    + exclude.substring(indexID + exclude.substring(indexID).indexOf("\n"))

                    : stExclude
                    ? exclude + id + " " + text.replace(command("exclude"), "") + "\n"

                    : eqForward
                    ? forward.contains(id) ? forward.replace(id, "") : forward + id

                    : same
                );
                //exclude.substring(indexID).indexOf("\n") рахує довжину рядка зі вказаним id,
                //тому функція продовжує запис, ігноруючи цей рядок

                enabled = (!stExclude || !exclude.toString().contains(id)) && enabled;

                String getText =
                        eqToggle
                        ? "бота " + (enabled ? "вимкнено" : "увімкнено") + " в чаті! "
                        + "щоб " + (enabled ? "увімкнути" : "вимкнути") + " знову: /toggle"

                        : eqExclude
                        ? "/exclude - команда, яка дозволяє виключати непотрібні вам слова в чаті.\n\n"
                        + "використання команди: /exclude слово1 слово2\n"
                        + "приклади:\n/exclude привет\n/exclude привет кто почему\n\n"
                        + "щоб видалити всі виключення:\n/exclude ."

                        : stExclude
                        ? "виключення записано!"

                        : eqForward
                        ? "видалення пересилань " + (forwardEnabled ? "вимкнено" : "увімкнено") + " в чаті! "
                        + "щоб " + (forwardEnabled ? "увімкнути" : "вимкнути") + " знову: /forward"

                        : "";

                send = !Objects.equals(getText, "") ? setText(getText) : send;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public String command(String cmd) {
        return "/" + cmd + (text.contains("@deadkacapbot") ? "@deadkacapbot " : " ");
    }
    public boolean equalsCommand(String command) {
    	return text.equals("/" + command) || text.equals("/" + command + "@deadkacapbot");
    }
    public void trimWordList() {
        int index = exclude.indexOf(id);
        boolean endFile = !exclude.substring(index).contains("\n");
        int endPoint = endFile
                ? exclude.length()
                : exclude.substring(index).indexOf("\n") + exclude.substring(0, index).length();
        String cropped = exclude.substring(index, endPoint);
        //ріже файл exclude.txt від id чату(включно) до \n, тобто доки не закінчиться рядок зі словами
        //якщо \n немає(кінець файлу), то бере до кінця файлу

        String[] excl = cropped.replace(id + " ", "").split(" ");
        for (String exc : excl) {
            getKacapWordsList.removeIf(exc::equals);
            getKacapWordsList.removeIf(exc::contains);
            getKacapWordsList2.removeIf(exc::equals);
            getKacapWordsList2.removeIf(exc::contains);
        }
    }
    public void kacapWords1() {
        for (String kacapWord : getKacapWordsList) {
            if (text.contains(kacapWord)) {
                kacap = true;
                send = setText("повідомлення видалено через " + (kacapWord.length() <= 2 ? "букву" : "слово")
                        + " \"" + kacapWord + "\"");
                break;
            }
        }
    }
    public void kacapWords2() {
        for (String kacapWord2 : getKacapWordsList2) {
            Pattern pattern = Pattern.compile("\\b" + kacapWord2 + "\\b");
            kacap = pattern.matcher(text).find();
            if (kacap) { send = setText("повідомлення видалено через слово \"" + kacapWord2 + "\""); break; }
        }
    }
    public void rusEng() {
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
    public void checkWords() {

        int j = 0;
        String[] inputsContains = {"слава Україні".toLowerCase(), "слава нації", "кацапи",
        "путін", "путин", "путлер", "путлєр"};
        String[] outputsContains = {"героям слава!", "смерть кацапам!", "кацапи - нелюди!", "путін - хуйло! кацапи - нелюди!"};
        for (int i = 0; i < inputsContains.length; i++) {
            send = text.contains(inputsContains[i]) ? setText(outputsContains[j]) : send;
            j += i > 2 ? 0 : 1;
        }

        String[] inputsEquals = {"кацапи", "Україна".toLowerCase()};
        String[] outputsEquals = {"нелюди!", "понад усе!"};
        for (int i = 0; i < inputsEquals.length; i++) {
            send = text.equals(inputsEquals[i]) ? setText(outputsEquals[i]) : send;
        }

        if (send) {
            sm.setReplyMarkup(sm.getReplyMarkup());
            sm.setReplyToMessageId(messageId);
        }
    }
    public boolean isAdmin() {
        final boolean[] admin = {false};
        try {
            execute(new GetChatAdministrators(id)).forEach(chatMember1 ->
                    admin[0] = admin[0] || message.getFrom().getId().equals(chatMember1.getUser().getId()));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return admin[0];
    }
    public boolean canBotDelete() {
        final boolean[] canDelete = {false};
        try {
            execute(new GetChatAdministrators(id)).forEach(chatMember ->
                    canDelete[0] = canDelete[0] || (chatMember.getUser().getId().equals(botID)
                    && chatMember.toString().contains("canDeleteMessages=true")));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return canDelete[0];
    }
    public void appendLog(int num) { log.append(kacap || send ? num + " " + kacap + " " + send + "\n" : ""); }
    public void displayWriteLog(Exception e) {
        String answer = e.getMessage();
        boolean ignore = answer.contains("have no rights to send a message");
        String[] errors = {"message can't be deleted", "message to delete not found", "replied message not found"};
        String[] answers = {"повідомлення не може бути видалене", "повідомлення не знайдено",
                "повідомлення для відповіді не знайдене"};
        for (int i = 0; i < errors.length; i++) {
            if (answer.contains(errors[i])) { answer = answers[i]; break; }
        }
        String error = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                + answer + ": " + chat.getTitle() +
                (chatUsername != null ? "; https://t.me/" + chatUsername
                + "/" + messageId : "") + "\n";
        System.out.print(error);
        if (!ignore) { append("log.txt", error); }
    }
    static String[] kacapWordsList = (
    "э ы ъ ё ьі " +
    "заебись ебат ебал тупой пидорас пидарас нихуя хуел еблан хуеть привет здаров здравствуй спасибо слуша " +
    "работ свободн почему тогда когда только почт пример русс росси понял далее запрет добавь другой совсем " +
    "понятн брос освобо согл хотел наверн мальчик девочк здрасте надеюс вреш скольк поздр разговари нрав слуша " +
    "удобн смотр общ админ делать делай извини удали наконец сложн ребят хохол хохл аниме короче нафиг").split(" ");
    static ArrayList<String> getKacapWordsList;
    static String[] kacapWordsList2 = (
    "да как кто никто некто его ето она оно они их еще что што пон поняв нипон непон кринж " +
    "какой какие каких нет однако пока если меня тебя сегодня и иди потом дашь пиздец лет мне ищу надо мой твой " +
    "свои свой зачем нужно надо всем есть ебет сейчас ща щя щас щяс либо может любой любая че чего где везде " +
    "игра играть играю двое трое хорошо улиц улица улице пиздос пошел пошла дела дело делаешь ваще срочно " +
    "жду ждать ждешь даже ребята пожалуйста вдруг помоги помогите помощь помог поможет помогла хуже играеш нужен будешь " +
    "хочешь пишите умею хотя нашел нашла удачи нету живая живой года лет прости простите нечто ничто ничего " +
    "такое такой такие такая пидр пидор пидар пидорас пидарас дебил дибил тварь время жизнь лучше ").split(" ");
    static ArrayList<String> getKacapWordsList2;
    public static void kacapWordsListTranslit() {
        Transliterator toLatinTrans = Transliterator.getInstance("Russian-Latin/BGN");
        for (String kacapWord : kacapWordsList) {
            System.out.print(toLatinTrans.transliterate(kacapWord) + " ");
        }
        for (String kacapWord2 : kacapWordsList2) {
            System.out.print(toLatinTrans.transliterate(kacapWord2) + " ");
        }
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
    public User getBot() {
        try {
            bot = execute(new GetMe());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return bot;
    }
}
