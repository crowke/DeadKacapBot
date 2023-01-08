# DeadKacapBot

бот, який видаляє повідомлення російською.
<br>
<br>

## що вміє?

весь написаний мною код знаходиться тут: [helloWorld.java](https://github.com/qlspd/DeadKacapBot/blob/main/src/main/java/com/tgBot/deadKacap/helloWorld.java)
- `kacapWords1` - банальна перевірка на text.contains(слово або буква зі словника kacapWordsList)
- `kacapWords2` - перевірка на точне співпадіння по слову зі словника kacapWordsList2<br>(тобто не `contains("привет")`, а `startsWith("привет ") || endsWith(" привет") || equals("привет") || contains(" привет ")`).<br>ігнорує більшість символів пунктуації, тому видалення зазвичай успішне
- `rusEng` - перевірка на з'єднання латинських букв із кирилицею в тексті(не враховуючи пунктуацію)
- `checkWords` - відповідає на фрази "слава Україні", "слава нації" або "путін" відповідними фразами в коді. включає в себе `ReplyMarkup`
- `/start` - якщо `enabled`, видає список команд. в іншому випадку повідомляє про те, що бота вимкнено(через `/toggle`)
- `/toggle` - вмикає/вимикає бота
- `/exclude` - дозволяє модифікувати словник індивідуально під чат.<br>приклад: `/exclude привет` - бот перестане реагувати на "привет" у цьому чаті
- `/forward` - вмикає/вимикає видалення пересланих повідомлень<br>(так як люди інколи відправляють новини російською, він їх без `/forward` буде теж видаляти).
---
бот записує логи про видалені повідомлення, для більшого розуміння помилок, які з ним відбуваються при оновленні коду.
<br>
<br>
також видає помилку в чат при відсутності можливості видалення повідомлень, пропонуючи вимкнути бота з допомогою `/toggle` або надати йому права на видалення.
<br>
<br>

## як заставити його працювати?
- java 11 і gradle

- змінити [application.properties](https://github.com/qlspd/DeadKacapBot/blob/main/src/main/resources/application.properties)<br>
telegram.bot.username=DeadKacapBot<br>
telegram.bot.token=1234567890:AAAA-5JdnvoSdovP49SnCvxXSK7A2NnM<br>

- `cd DeadKacapBot`
- `gradle bootRun`
