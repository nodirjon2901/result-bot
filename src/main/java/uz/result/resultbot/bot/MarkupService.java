package uz.result.resultbot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.result.resultbot.model.Basket;
import uz.result.resultbot.model.Language;
import uz.result.resultbot.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


@Service
@RequiredArgsConstructor
public class MarkupService {

    private final UserService userService;

    public InlineKeyboardMarkup selectLanguageInlineMarkup() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonRow = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Uzb\uD83C\uDDFA\uD83C\uDDFF");
        button.setCallbackData("UZB");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText("Rus\uD83C\uDDF7\uD83C\uDDFA");
        button.setCallbackData("RUS");
        buttonRow.add(button);

        rowsInline.add(buttonRow);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public ReplyKeyboardMarkup functionReplyMarkup(Long chatId) throws ExecutionException, InterruptedException {
        ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
        replyKeyboard.setResizeKeyboard(true);
        replyKeyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        String buttonText1 = "";
        String buttonText2 = "";
        String buttonText3 = "";
        String buttonText4 = "";
        String buttonText5 = "";
        String buttonText6 = "";

        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            buttonText1 = "Xizmatlar 📈";
            buttonText2 = "Ariza qoldirish ✍️";
            buttonText3 = "Bog'lanish 👨🏼‍💻";
            buttonText4 = "Savat 🛒";
            buttonText5 = "Bizning kanal \uD83D\uDC49";
            buttonText6 = "Orqaga 🔙";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            buttonText1 = "Услуги 📈";
            buttonText2 = "Оставить заявку ✍️";
            buttonText3 = "Связаться 👨🏼‍💻";
            buttonText4 = "Корзина 🛒";
            buttonText5 = "Наш канал \uD83D\uDC49";
            buttonText6 = "Назад 🔙";
        }

        // First row
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(buttonText1));
        row1.add(new KeyboardButton(buttonText2));
        rows.add(row1);

        // Second row
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(buttonText3));
        row2.add(new KeyboardButton(buttonText4));
        rows.add(row2);

        // Third row
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(buttonText5));
        row3.add(new KeyboardButton(buttonText6));
        rows.add(row3);

        replyKeyboard.setKeyboard(rows);
        return replyKeyboard;
    }

    public ReplyKeyboardMarkup serviceReplyMarkup(Long chatId) throws ExecutionException, InterruptedException {
        ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
        replyKeyboard.setResizeKeyboard(true);
        replyKeyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        String buttonText1 = "";
        String buttonText2 = "";
        String buttonText3 = "";
        String buttonText4 = "";
        String buttonText5 = "";
        String buttonText6 = "";
        String buttonText7 = "";
        String buttonText8 = "";

        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            buttonText1 = "Saytlar \uD83D\uDDA5";
            buttonText2 = "Telegram-botlar \uD83D\uDCF2";
            buttonText3 = "SMM \uD83D\uDCF1";
            buttonText4 = "Reklama boshlash \uD83D\uDCB3";
            buttonText5 = "SEO \uD83E\uDEAA";
            buttonText6 = "Brending \uD83D\uDCA1";
            buttonText7 = "Savat 🛒";
            buttonText8 = "Orqaga 🔙";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            buttonText1 = "Сайты \uD83D\uDDA5";
            buttonText2 = "Telegram-боты \uD83D\uDCF2";
            buttonText3 = "SMM \uD83D\uDCF1";
            buttonText4 = "Запуск рекламы \uD83D\uDCB3";
            buttonText5 = "SEO \uD83E\uDEAA";
            buttonText6 = "Брендинг \uD83D\uDCA1";
            buttonText7 = "Корзина 🛒";
            buttonText8 = "Назад 🔙";
        }

        // First row
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(buttonText1));
        row1.add(new KeyboardButton(buttonText2));
        rows.add(row1);

        // Second row
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(buttonText3));
        row2.add(new KeyboardButton(buttonText4));
        rows.add(row2);

        // Third row
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(buttonText5));
        row3.add(new KeyboardButton(buttonText6));
        rows.add(row3);

        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton(buttonText7));
        row5.add(new KeyboardButton(buttonText8));
        rows.add(row5);

        replyKeyboard.setKeyboard(rows);
        return replyKeyboard;
    }

//    public InlineKeyboardMarkup serviceInlineMarkup(Long chatId) throws ExecutionException, InterruptedException {
//        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
//        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
//        List<InlineKeyboardButton> buttonRow = new ArrayList<>();
//
//        String buttonText1 = "";
//        String buttonText2 = "";
//        String buttonText3 = "";
//        String buttonText4 = "";
//        String buttonText5 = "";
//        String buttonText6 = "";
//        String buttonText7 = "";
//        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
//            buttonText1 = "Saytlar";
//            buttonText2 = "Telegram-botlar";
//            buttonText3 = "SMM";
//            buttonText4 = "Reklama boshlash";
//            buttonText5 = "SEO";
//            buttonText6 = "Brending";
//            buttonText7 = "Orqaga 🔙";
//        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
//            buttonText1 = "Сайты";
//            buttonText2 = "Telegram-боты";
//            buttonText3 = "SMM";
//            buttonText4 = "Запуск рекламы";
//            buttonText5 = "SEO";
//            buttonText6 = "Брендинг";
//            buttonText7 = "Назад 🔙";
//        }
//        InlineKeyboardButton button = new InlineKeyboardButton();
//        button.setText(buttonText1);
//        button.setCallbackData("site");
//        buttonRow.add(button);
//
//        button = new InlineKeyboardButton();
//        button.setText(buttonText2);
//        button.setCallbackData("bot");
//        buttonRow.add(button);
//
//        button = new InlineKeyboardButton();
//        button.setText(buttonText3);
//        button.setCallbackData("smm");
//        buttonRow.add(button);
//        rowsInline.add(buttonRow);
//
//        buttonRow = new ArrayList<>();
//        button = new InlineKeyboardButton();
//        button.setText(buttonText4);
//        button.setCallbackData("advertising");
//        buttonRow.add(button);
//
//        button = new InlineKeyboardButton();
//        button.setText(buttonText5);
//        button.setCallbackData("seo");
//        buttonRow.add(button);
//
//        button = new InlineKeyboardButton();
//        button.setText(buttonText6);
//        button.setCallbackData("branding");
//        buttonRow.add(button);
//        rowsInline.add(buttonRow);
//
//        buttonRow = new ArrayList<>();
//        button = new InlineKeyboardButton();
//        button.setText(buttonText7);
//        button.setCallbackData("back");
//        buttonRow.add(button);
//        rowsInline.add(buttonRow);
//
//        inlineKeyboard.setKeyboard(rowsInline);
//        return inlineKeyboard;
//    }

    public ReplyKeyboard serviceButtonInlineKeyboardMarkup(Long chatId) throws ExecutionException, InterruptedException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonRow = new ArrayList<>();
        String URL = "";
        String buttonText1 = "";
        String buttonText2 = "";
        String buttonText3 = "";
        String userState = UserSession.getUserStateTemporary(chatId).name();
        switch (userState) {
            case "SERVICE_SITE" -> URL = "https://result-me.uz/ru/services/web-development";
            case "SERVICE_SEO" -> URL = "https://result-me.uz/ru/services/seo";
            case "SERVICE_SMM" -> URL = "https://result-me.uz/ru/services/smm";
            case "SERVICE_BOT" -> URL = "https://result-me.uz/ru/services/telegram-bot-development";
            case "SERVICE_BRANDING" -> URL = "https://result-me.uz/ru/services/branding";
            case "SERVICE_ADVERTISING" -> URL = "https://result-me.uz/ru/services/ads-launch";
        }

        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            buttonText1 = "Ba'tafsil \uD83D\uDC49";
            buttonText2 = "Savatga 🛒";
            buttonText3 = "Orqaga 🔙";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            buttonText1 = "Подробнее \uD83D\uDC49";
            buttonText2 = "В корзину 🛒";
            buttonText3 = "Назад 🔙";
        }
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText1);
        button.setUrl(URL);
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(buttonText2);
        button.setCallbackData("basket");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(buttonText3);
        button.setCallbackData("back");
        buttonRow.add(button);

        rowsInline.add(buttonRow);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup interestedServiceInlineMarkup(Long chatId) throws ExecutionException, InterruptedException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Saytlar");
            button.setCallbackData("Saytlar");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Telegram-botlar");
            button.setCallbackData("Telegram-botlar");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("SMM");
            button.setCallbackData("SMM");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Reklama boshlash");
            button.setCallbackData("Reklama boshlash");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("SEO");
            button.setCallbackData("SEO");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Brending");
            button.setCallbackData("Brending");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Keyingi ⏭");
            button.setCallbackData("next");
            rowsInline.add(List.of(button));
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Сайты");
            button.setCallbackData("Сайты");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Telegram-боты");
            button.setCallbackData("Telegram-боты");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("SMM");
            button.setCallbackData("SMM");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Запуск рекламы");
            button.setCallbackData("Запуск рекламы");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("SEO");
            button.setCallbackData("SEO");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Брендинг");
            button.setCallbackData("Брендинг");
            rowsInline.add(List.of(button));

            button = new InlineKeyboardButton();
            button.setText("Следующий ⏭");
            button.setCallbackData("next");
            rowsInline.add(List.of(button));
        }
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup appSelectedServiceButtonInlineMarkup(Long chatId) throws ExecutionException, InterruptedException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonRow = new ArrayList<>();

        String buttonText1 = "";
        String buttonText2 = "";
        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            buttonText1 = "OK ✅";
            buttonText2 = "Orqaga \uD83D\uDD19";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            buttonText1 = "ОК ✅";
            buttonText2 = "Назад \uD83D\uDD19";
        }
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText1);
        button.setCallbackData("send");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(buttonText2);
        button.setCallbackData("back");
        buttonRow.add(button);

        rowsInline.add(buttonRow);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;

    }

    public InlineKeyboardMarkup sendToUserAppButtonsInlineMarkup(Long chatId) throws ExecutionException, InterruptedException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonRow = new ArrayList<>();

        String buttonText1 = "";
        String buttonText2 = "";
        String buttonText3 = "";
        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            buttonText1 = "Yuborish ✅";
            buttonText2 = "Tahrirlash ✏️";
            buttonText3 = "Orqaga \uD83D\uDD19";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            buttonText1 = "Отправить ✅";
            buttonText2 = "Редактировать ✏️";
            buttonText3 = "Назад \uD83D\uDD19";
        }
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText1);
        button.setCallbackData("send");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(buttonText2);
        button.setCallbackData("edit");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(buttonText3);
        button.setCallbackData("back");
        buttonRow.add(button);

        rowsInline.add(buttonRow);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;

    }

    public InlineKeyboardMarkup basketFunctionInlineMarkup(Long chatId) throws ExecutionException, InterruptedException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Tanlangan xizmatlar ro‘yxati ko'rish \uD83D\uDCCB");
            button.setCallbackData("selected_service_list");
            rowsInline.add(List.of(button));
            button = new InlineKeyboardButton();
            button.setText("Orqaga \uD83D\uDD19");
            button.setCallbackData("back");
            rowsInline.add(List.of(button));
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Посмотреть список выбранных услуг \uD83E\uDDFE");
            button.setCallbackData("selected_service_list");
            rowsInline.add(List.of(button));
            button = new InlineKeyboardButton();
            button.setText("Назад \uD83D\uDD19");
            button.setCallbackData("back");
            rowsInline.add(List.of(button));

        }
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup selectServiceListInBasketFunctionInlineMarkup(Long chatId, Basket basket) throws ExecutionException, InterruptedException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonRow;
        for (String name : basket.getService()) {
            buttonRow = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(name);
            button.setCallbackData("none");
            buttonRow.add(button);
            button = new InlineKeyboardButton();
            button.setText("❌");
            button.setCallbackData("del_" + name);
            buttonRow.add(button);
            rowsInline.add(buttonRow);
        }
        String button1 = "";
        String button2 = "";
        String button3 = "";
        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            button1 = "Tijorat taklifini olish \uD83E\uDDFE";
            button2 = "Savatni tozalash \uD83D\uDDD1";
            button3 = "Orqaga \uD83D\uDD19";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            button1 = "Получить коммерческое предложение  \uD83D\uDCCB";
            button2 = "Очистить корзину \uD83D\uDDD1";
            button3 = "Назад \uD83D\uDD19";
        }
        InlineKeyboardButton button = new InlineKeyboardButton();
        buttonRow = new ArrayList<>();
        button.setText(button1);
        button.setCallbackData("commercial");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(button2);
        button.setCallbackData("clear_basket");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(button3);
        button.setCallbackData("back");
        buttonRow.add(button);

        rowsInline.add(buttonRow);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup commercialOfferButtonsInlineKeyboardMarkup(Long chatId) throws ExecutionException, InterruptedException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonRow = new ArrayList<>();

        String buttonText1 = "";
        String buttonText2 = "";
        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            buttonText1 = "OK ✅";
            buttonText2 = "Yana ➕";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            buttonText1 = "ОК ✅";
            buttonText2 = "Снова ➕";
        }
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText1);
        button.setCallbackData("send");
        buttonRow.add(button);

        button = new InlineKeyboardButton();
        button.setText(buttonText2);
        button.setCallbackData("add");
        buttonRow.add(button);

        rowsInline.add(buttonRow);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public ReplyKeyboardRemove replyKeyboardRemove() {
        return new ReplyKeyboardRemove(true);
    }

}
