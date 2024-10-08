package uz.result.resultbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import uz.result.resultbot.model.*;
import uz.result.resultbot.service.ApplicationService;
import uz.result.resultbot.service.BasketService;
import uz.result.resultbot.service.CommercialOfferService;
import uz.result.resultbot.service.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HandleService {

    private final UserService userService;

    private final BasketService basketService;

    private final MarkupService markupService;

    private final ApplicationService applicationService;

    private final CommercialOfferService commercialService;

    @Value("${group.chatId}")
    private String GROUP_CHAT_ID;

    @Value("${photo.file.path}")
    private String PHOTO_PATH;

    @SneakyThrows
    public void defaultMessageHandler(Long chatId, String text, TelegramLongPollingBot bot) {
        if (text.equals("/start")) {
            User user = User.builder()
                    .chatId(chatId)
                    .language(Language.RUS)
                    .userState(UserState.START)
                    .build();
            userService.save(user);
            UserSession.updateUserState(chatId, UserState.START);
            startMessageHandler(chatId, bot);
            return;
        }
        SendMessage sendMessage = new SendMessage(
                chatId.toString(),
                """
                        Boshlash uchun /start ni bosing\s
                        \s
                        Нажмите /start, чтобы начать
                        """);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId, bot);
        removeUserWriteMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void startMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage(
                chatId.toString(),
                """
                        Assalomu alaykum. Tilni tanlang\s
                        \s
                        Привет. Выберите язык"""
        );
        sendMessage.setReplyMarkup(markupService.selectLanguageInlineMarkup());
        Message message = bot.execute(sendMessage);
        removeMessage(chatId, bot);
        removeUserWriteMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }


    @SneakyThrows
    public void notSupportedMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        String text = "";
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            text = "Iltimos savolingizni faqat yozma ravishda yo'llang!";
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            text = "Пожалуйста, задавайте свой вопрос только в письменном виде!";
        SendMessage sendMessage = new SendMessage(chatId.toString(), text);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void menuMessageHandler(Long chatId, String data, TelegramLongPollingBot bot) {
        userService.changeLanguage(chatId, data);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Marhamat quyidagi funksiyalardan birini tanlang");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Пожалуйста, выберите одну из функций ниже");
        sendMessage.setReplyMarkup(markupService.functionReplyMarkup(chatId));
        if (!UserSession.getUserStateTemporary(chatId).equals(UserState.COMMERCIAL_SEND_USER)) {
            removeMessage(chatId, bot);
        }
        UserSession.updateUserState(chatId, UserState.SELECT_FUNCTION);
        Message message = bot.execute(sendMessage);
        removeUserWriteMessage(chatId, bot);
        removeWarningMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void backOperationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        UserState userState = UserSession.getUserStateTemporary(chatId);
        if (userState.equals(UserState.SELECT_FUNCTION)) {
            UserSession.updateUserState(chatId, UserState.START);
            startMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.FUNCTION_SERVICE)) {
            UserSession.updateUserState(chatId, UserState.SELECT_FUNCTION);
            String data = userService.getLanguage(chatId).get().name();
            menuMessageHandler(chatId, data, bot);
        } else if (userState.equals(UserState.SERVICE_ADVERTISING) || userState.equals(UserState.SERVICE_BOT) || userState.equals(UserState.SERVICE_BRANDING) ||
                userState.equals(UserState.SERVICE_SEO) || userState.equals(UserState.SERVICE_SMM) || userState.equals(UserState.SERVICE_SITE)) {
            UserSession.updateUserState(chatId, UserState.FUNCTION_SERVICE);
            serviceSelectFunctionMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.APP_SELECTED_SERVICE)) {
            UserSession.updateUserState(chatId, UserState.APP_SERVICE);
            applicationService.clearServices(chatId);
            String phoneNumber = UserSession.getApplication(chatId).getPhoneNumber();
            appPhoneNumMessageHandler(chatId, phoneNumber, bot);
        } else if (userState.equals(UserState.APP_SEND_USER)) {
            UserSession.updateUserState(chatId, UserState.APP_SELECTED_SERVICE);
            selectAppServiceMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.FUNCTION_BASKET)) {
            UserSession.updateUserState(chatId, UserState.SELECT_FUNCTION);
            menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
        } else if (userState.equals(UserState.FUNCTION_BASKET_SELECTED_SERVICE)) {
            UserSession.updateUserState(chatId, UserState.FUNCTION_BASKET);
            basketMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.COMMERCIAL_SELECTED_SERVICE)) {
            UserSession.updateUserState(chatId, UserState.COMMERCIAL_SERVICE);
            commercialCurrentServiceMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.COMMERCIAL_SEND_USER)) {
            UserSession.updateUserState(chatId, UserState.COMMERCIAL_SELECTED_SERVICE);
            selectBasketServiceMessageHandler(chatId, bot);
        }
    }

    @SneakyThrows
    public void serviceSelectFunctionMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Bizning xizmatlar");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Наши услуги");
        sendMessage.setReplyMarkup(markupService.replyKeyboardRemove());
        sendMessage.setReplyMarkup(markupService.serviceReplyMarkup(chatId));
        UserSession.updateUserState(chatId, UserState.FUNCTION_SERVICE);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId,bot);
        removeUserWriteMessage(chatId,bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());

    }

    @SneakyThrows
    public void siteServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        File imageFile = new File(PHOTO_PATH + "Site.jpg");
        sendPhoto.setPhoto(new InputFile(imageFile));
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendPhoto.setCaption("*Foydalanishda qulay va professional ko‘rinishga ega saytlarni ishlab chiqish, bu biznesingizni internetda ajralib turishiga yordam beradi.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendPhoto.setCaption("*Разработка сайтов, которые просты в использовании и выглядят профессионально, помогая бизнесу выделяться в интернете.*");
        sendPhoto.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.SERVICE_SITE);
        sendPhoto.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        bot.execute(sendPhoto);
        removeMessage(chatId, bot);
    }


    @SneakyThrows
    public void botServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        File imageFile = new File(PHOTO_PATH + "Tg bot.jpg");
        sendPhoto.setPhoto(new InputFile(imageFile));
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendPhoto.setCaption("*Mijozlar bilan muloqotni osonlashtiradigan va oddiy jarayonlarni avtomatlashtiradigan Telegram-botlar yaratish.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendPhoto.setCaption("*Создание Telegram-ботов, которые облегчают общение с клиентами и автоматизируют рутинные процессы.*");
        sendPhoto.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.SERVICE_BOT);
        sendPhoto.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        bot.execute(sendPhoto);
        removeMessage(chatId, bot);
    }

    @SneakyThrows
    public void smmServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        File imageFile = new File(PHOTO_PATH + "SMM.jpg");
        sendPhoto.setPhoto(new InputFile(imageFile));
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendPhoto.setCaption("*Ijtimoiy tarmoqlarni yaratish va boshqarishda yordam berish, auditoriyani kengaytirish va brend imidjini yaxshilash.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendPhoto.setCaption("*Помощь в создании и управлении социальными сетями, чтобы увеличить аудиторию и улучшить имидж бренда.*");
        sendPhoto.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.SERVICE_SMM);
        sendPhoto.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        bot.execute(sendPhoto);
        removeMessage(chatId, bot);
    }

    @SneakyThrows
    public void advertisingServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        File imageFile = new File(PHOTO_PATH + "Add.jpg");
        sendPhoto.setPhoto(new InputFile(imageFile));
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendPhoto.setCaption("*Kerakli mijozlarni jalb qiladigan va biznesingizga ko‘proq foyda keltiradigan reklamani sozlash.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendPhoto.setCaption("*Настройка рекламы, которая привлекает нужных клиентов и приносит бизнесу больше прибыли.*");
        sendPhoto.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.SERVICE_ADVERTISING);
        sendPhoto.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        bot.execute(sendPhoto);
        removeMessage(chatId, bot);
    }

    @SneakyThrows
    public void seoServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        File imageFile = new File(PHOTO_PATH + "SEO.jpg");
        sendPhoto.setPhoto(new InputFile(imageFile));
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendPhoto.setCaption("*Saytingizni qidiruv tizimlarida yuqoriroq bo‘lishi va ko‘proq tashrif buyuruvchilarni jalb qilishi uchun optimallashtirish.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendPhoto.setCaption("*Оптимизация сайта, чтобы он был выше в поисковых системах и привлекал больше посетителей.*");
        sendPhoto.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.SERVICE_SEO);
        sendPhoto.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        bot.execute(sendPhoto);
        removeMessage(chatId, bot);
    }

    @SneakyThrows
    public void brandingServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        File imageFile = new File(PHOTO_PATH + "Branding.jpg");
        sendPhoto.setPhoto(new InputFile(imageFile));
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendPhoto.setCaption("*Biznesingiz uchun o‘ziga xos uslub va obraz yaratish, bu esa mijozlar uchun oson tanib olinadigan va jozibali bo‘lishi uchun.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendPhoto.setCaption("*Создание уникального стиля и образа для бизнеса, который будет легко узнаваем и привлекателен для клиентов.*");
        sendPhoto.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.SERVICE_BRANDING);
        sendPhoto.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        bot.execute(sendPhoto);
        removeMessage(chatId, bot);
    }

    @SneakyThrows
    public void applicationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Ism sharifingiz kiriting (F.I.O): ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите свое полное имя (Ф.И.O): ");
        UserSession.updateUserState(chatId, UserState.APP_FULL_NAME);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId, bot);
        removeUserWriteMessage(chatId,bot);
        UserSession.updateAppMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void appFullNameMessageHandler(Long chatId, String fullName, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Telefon raqam kiriting: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите номер телефона: ");
        applicationService.updateFullName(fullName, chatId);
        UserSession.updateUserState(chatId, UserState.APP_PHONE_NUMBER);
        Message message = bot.execute(sendMessage);
        UserSession.updateAppMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void appPhoneNumMessageHandler(Long chatId, String phoneNum, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Qiziqtirgan xizmatlarni tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите интересующие вас услуги: ");
        sendMessage.setReplyMarkup(markupService.interestedServiceInlineMarkup(chatId));
        applicationService.updatePhoneNum(phoneNum, chatId);
        UserSession.updateUserState(chatId, UserState.APP_SERVICE);
        Message message = bot.execute(sendMessage);
        removeAppSpecialMessage(chatId, bot);
        removeMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void selectAppServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Application application = UserSession.getApplication(chatId);
        if (application.getService() == null || application.getService().isEmpty()) {
            shouldSelectServiceMessageHandler(chatId, bot);
            return;
        }
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Siz tanlagan xizmatlar:*\n\n" + "_" + formattedSelectAppServices(application.getService()) + "_\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Услуги, которые вы выбираете:*\n\n" + "_" + formattedSelectAppServices(application.getService()) + "_\n");
        sendMessage.setReplyMarkup(markupService.appSelectedServiceButtonInlineMarkup(chatId));
        sendMessage.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.APP_SELECTED_SERVICE);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    private void shouldSelectServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Qiziqtirgan xizmatlarni tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите интересующие вас услуги: ");
        sendMessage.setReplyMarkup(markupService.interestedServiceInlineMarkup(chatId));
        UserSession.updateUserState(chatId, UserState.APP_SERVICE);
        Message message = bot.execute(sendMessage);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    private String formattedSelectAppServices(Set<String> services) {
        StringBuilder text = new StringBuilder();
        for (String service : services) {
            text.append(service).append("\n");
        }
        return text.toString();
    }

    private String formattedAppServices(Set<String> services) {
        StringBuilder text = new StringBuilder();
        for (String service : services) {
            text.append(service).append(", ");
        }
        if (!text.isEmpty()) {
            text.setLength(text.length() - 2);
        }
        return text.toString();
    }

    @SneakyThrows
    public void sendToUserApplicationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Application application = UserSession.getApplication(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Sizning arizangiz:* \n\n" + formattedApplication(chatId, application) + "\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Ваше приложение:* \n\n" + formattedApplication(chatId, application) + "\n");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.sendToUserAppButtonsInlineMarkup(chatId));
        UserSession.updateUserState(chatId, UserState.APP_SEND_USER);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    private String formattedApplication(Long chatId, Application application) {
        String text = "";
        if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            text = "👤 Ф.И.О: " + application.getFullName() + "\n" +
                    "📞 Номер телефона: " + application.getPhoneNumber() + "\n" +
                    "📈 Интересующая услуга: " + formattedAppServices(application.getService()) + "\n";
        } else if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            text = "👤 F.I.O: " + application.getFullName() + "\n" +
                    "📞 Telefon nomer: " + application.getPhoneNumber() + "\n" +
                    "📈 Qiziqtirgan xizmatlar: " + formattedAppServices(application.getService()) + "\n";
        }
        return text;
    }

    @SneakyThrows
    public void sendToDatabaseApplicationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Application application = applicationService.setUserInApplication(chatId, userService.findByChatId(chatId));
        Application save = applicationService.save(application);
        sendApplicationToCompanyGroup(bot, save);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Arizangiz qabul qilindi* ✅");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Ваша заявка принята* ✅");
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);
        UserSession.removeUserMessageId(chatId);
        UserSession.removeUserState(chatId);
        menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
        UserSession.removeApplication(chatId);
        UserSession.clearMessageChatId(chatId);
    }

    private void sendApplicationToCompanyGroup(TelegramLongPollingBot bot, Application application) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(GROUP_CHAT_ID);
        sendMessage.setText(
                "*Новое заявка \uD83D\uDCCB*\n\n" +
                        "*👤 Ф.И.О:* " + application.getFullName() + "\n" +
                        "*📞 Номер телефона:* " + application.getPhoneNumber() + "\n" +
                        "*📈 Интересующая услуга: *" + formattedAppServices(application.getService()) + "\n\n"
        );
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);


        String jsonBody = createNotionJson(application);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("https://api.notion.com/v1/pages"))
                .header("Authorization", "Bearer " + "secret_lnLNl6lfhuBnnZmYOlg6tUGXUv8VjxmnDCffo0wwKJS")
                .header("Content-Type", "application/json")
                .header("Notion-Version", "2022-06-28")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();


        try {
            client.send(request1, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    private String createNotionJson(Application application) {
        return "{"
                + "\"parent\": { \"database_id\": \"" + "2b43ccc3b4af4e53a8e6802fb3f7fcfb" + "\" },"
                + "\"properties\": {"
                + "\"Name\": {"
                + "\"title\": [{ \"text\": { \"content\": \"" + escapeJson(application.getFullName()) + "\" } }]"
                + "},"
                + "\"Type of Service\": {"
                + "\"rich_text\": [{ \"text\": { \"content\": \"" + escapeJson(application.getService().toString()) + "\" } }]"
                + "},"
                + "\"Phone\": {"
                + "\"phone_number\": \"" + application.getPhoneNumber() + "\""
                + "}"
                + "}"
                + "}";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return null;
        }
        // JSON maxsus belgilarini o'zlashtirish
        return value.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("/", "\\/")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SneakyThrows
    public void basketOperationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        UserState userState = UserSession.getUserStateTemporary(chatId);
        Basket basket = basketService.findByChatId(chatId);
        if (basket == null)
            basket = UserSession.getBasket(chatId);
        if (basket.getService() == null)
            basket.setService(new HashSet<>());
        String text = "";

        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            switch (userState) {
                case SERVICE_SEO -> basket.getService().add("SEO");
                case SERVICE_SITE -> basket.getService().add("Saytlar");
                case SERVICE_SMM -> basket.getService().add("SMM");
                case SERVICE_BOT -> basket.getService().add("Telegram-botlar");
                case SERVICE_BRANDING -> basket.getService().add("Brending");
                case SERVICE_ADVERTISING -> basket.getService().add("Reklama boshlash");
            }
            text = "Tanlangan xizmat savatga qo'shildi✅";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            switch (userState) {
                case SERVICE_SEO -> basket.getService().add("SEO");
                case SERVICE_SITE -> basket.getService().add("Сайты");
                case SERVICE_SMM -> basket.getService().add("SMM");
                case SERVICE_BOT -> basket.getService().add("Telegram-боты");
                case SERVICE_BRANDING -> basket.getService().add("Брендинг");
                case SERVICE_ADVERTISING -> basket.getService().add("Запуск рекламы");
            }
            text = "Выбранная услуга добавлена в корзину✅";
        }
        basket.setUser(userService.findByChatId(chatId));
        basketService.save(basket);
        UserSession.removeBasket(chatId);
        SendMessage sendMessage = new SendMessage(chatId.toString(), text);
        bot.execute(sendMessage);
        serviceSelectFunctionMessageHandler(chatId, bot);
    }

    @SneakyThrows
    public void basketMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Quyidagi funksiyalardan birini tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите одну из функций ниже: ");
        sendMessage.setReplyMarkup(markupService.basketFunctionInlineMarkup(chatId));
        UserSession.updateUserState(chatId, UserState.FUNCTION_BASKET);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId,bot);
        removeUserWriteMessage(chatId,bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void selectServiceListInBasketFunction(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket basket = basketService.findByChatId(chatId);
        if (basket == null || (basket.getService() == null || basket.getService().isEmpty())) {
            if (userService.getLanguage(chatId).get().equals(Language.UZB))
                sendMessage.setText("*Tanlangan xizmatlar mavjud emas*‼️");
            else if (userService.getLanguage(chatId).get().equals(Language.RUS))
                sendMessage.setText("*Выбранные услуги недоступны*‼️");
            sendMessage.setParseMode("Markdown");
            removeMessage(chatId, bot);
            removeWarningMessage(chatId,bot);
            Message message = bot.execute(sendMessage);
            UserSession.updateUserWarningMessageId(chatId, message.getMessageId());
            basketMessageHandler(chatId, bot);
            return;
        }
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Siz tanlagan xizmatlar: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Услуги, которые вы выбираете: ");
        sendMessage.setReplyMarkup(markupService.selectServiceListInBasketFunctionInlineMarkup(chatId, basket));
        UserSession.updateUserState(chatId, UserState.FUNCTION_BASKET_SELECTED_SERVICE);
        removeMessage(chatId, bot);
        Message message = bot.execute(sendMessage);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    public boolean checkData(String data) {
        return data.startsWith("del_");
    }

    public void deleteBasketService(Long chatId, String data, TelegramLongPollingBot bot) {
        Basket basket = basketService.findByChatId(chatId);
        String service = data.substring(4);
        basket.getService().removeIf(basketService -> basketService.equals(service));
        basketService.save(basket);
        selectServiceListInBasketFunction(chatId, bot);
    }

    @SneakyThrows
    public void clearUserBasketMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        Basket basket = basketService.findByChatId(chatId);
        basket.getService().removeAll(basket.getService());
        basketService.save(basket);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Savat tozalandi*‼️");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Корзина очищена*‼️");
        sendMessage.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.FUNCTION_BASKET);
        removeMessage(chatId, bot);
        Message message = bot.execute(sendMessage);
        UserSession.updateUserWarningMessageId(chatId, message.getMessageId());
        basketMessageHandler(chatId, bot);
    }

    @SneakyThrows
    public void commercialMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Ism sharifingiz kiriting (F.I.O): ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите свое полное имя (Ф.И.O): ");
        UserSession.updateUserState(chatId, UserState.COMMERCIAL_FULL_NAME);
        Message message = bot.execute(sendMessage);
        UserSession.updateSpecialMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void commercialFullNameMessageHandler(Long chatId, String fullName, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Telefon raqam kiriting: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите номер телефона: ");
        commercialService.updateFullName(fullName, chatId);
        UserSession.updateUserState(chatId, UserState.COMMERCIAL_PHONE_NUMBER);
        Message message = bot.execute(sendMessage);
        UserSession.updateSpecialMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void commercialPhoneNumMessageHandler(Long chatId, String phoneNum, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket basket = basketService.findByChatId(chatId);
        if (basket != null && (basket.getService() != null && !basket.getService().isEmpty())) {
            oldSelectedCommercialServiceMessageHandler(chatId, bot, basket);
            commercialService.updatePhoneNum(phoneNum, chatId);
            return;
        }
        commercialService.updatePhoneNum(phoneNum, chatId);
        commercialCurrentServiceMessageHandler(chatId, bot);
    }

    @SneakyThrows
    public void commercialCurrentServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Qiziqtirgan xizmatlarni tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите интересующие вас услуги: ");
        sendMessage.setReplyMarkup(markupService.interestedServiceInlineMarkup(chatId));
        UserSession.updateUserState(chatId, UserState.COMMERCIAL_SERVICE);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    public void oldSelectedCommercialServiceMessageHandler(Long chatId, TelegramLongPollingBot bot, Basket basket) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Siz tanlagan xizmatlar:* " + formatBasketService(basket.getService()));
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Услуги, которые вы выбираете:* " + formatBasketService(basket.getService()));
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.commercialOfferButtonsInlineKeyboardMarkup(chatId));
        UserSession.updateUserState(chatId, UserState.COMMERCIAL_OLD_SERVICE);
        Message message = bot.execute(sendMessage);
        removeSpecialMessage(chatId, bot);
        removeMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    private String formatBasketService(Set<String> services) {
        StringBuilder text = new StringBuilder();
        for (String service : services)
            text.append(service).append(", ");
        if (!text.isEmpty())
            text.setLength(text.length() - 2);
        return text.toString();
    }

    @SneakyThrows
    public void selectBasketServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket basket = UserSession.getBasket(chatId);
        if (basket.getService() == null) {
            basket.setService(new HashSet<>());
        }
        Basket dbBasket = basketService.findByChatId(chatId);
        basket.getService().addAll(dbBasket.getService());
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Siz tanlagan xizmatlar:*\n\n" + "_" + formatBasketService(basket.getService()) + "_\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Услуги, которые вы выбираете:*\n\n" + "_" + formatBasketService(basket.getService()) + "_\n");
        sendMessage.setReplyMarkup(markupService.appSelectedServiceButtonInlineMarkup(chatId));
        sendMessage.setParseMode("Markdown");
        UserSession.updateUserState(chatId, UserState.COMMERCIAL_SELECTED_SERVICE);
        bot.execute(sendMessage);
        removeMessage(chatId, bot);
    }

    @SneakyThrows
    public void sendToUserCommercialMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket dbBasket = basketService.findByChatId(chatId);
        Basket basket = UserSession.getBasket(chatId);
        if (basket != null) {
            if (basket.getService() != null && !basket.getService().isEmpty()) {
                dbBasket.getService().addAll(basket.getService());
                dbBasket = basketService.update(dbBasket);
            }
        }
        CommercialOffer commercialOffer = UserSession.getCommercialOffer(chatId);
        if (commercialOffer.getService() == null) {
            commercialOffer.setService(new HashSet<>());
        }
        commercialOffer.getService().addAll(dbBasket.getService());
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Tijorat taklifi:* \n\n" + formattedCommercial(chatId, commercialOffer) + "\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Коммерческое предложение:* \n\n" + formattedCommercial(chatId, commercialOffer) + "\n");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.sendToUserAppButtonsInlineMarkup(chatId));
        UserSession.updateUserState(chatId, UserState.COMMERCIAL_SEND_USER);
        Message message = bot.execute(sendMessage);
        removeMessage(chatId, bot);
        UserSession.updateUserMessageId(chatId, message.getMessageId());
    }

    @SneakyThrows
    private String formattedCommercial(Long chatId, CommercialOffer commercialOffer) {
        String text = "";
        if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            text = "👤 Ф.И.О: " + commercialOffer.getFullName() + "\n" +
                    "📞 Номер телефона: " + commercialOffer.getPhoneNumber() + "\n" +
                    "📈 Интересующая услуга: " + formattedAppServices(commercialOffer.getService()) + "\n";
        } else if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            text = "👤 F.I.O: " + commercialOffer.getFullName() + "\n" +
                    "📞 Telefon nomer: " + commercialOffer.getPhoneNumber() + "\n" +
                    "📈 Qiziqtirgan xizmatlar: " + formattedAppServices(commercialOffer.getService()) + "\n";
        }
        return text;
    }

    @SneakyThrows
    public void sendToDatabaseCommercialMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        CommercialOffer commercialOffer = commercialService.setUserInCommercial(chatId, userService.findByChatId(chatId));
        CommercialOffer save = commercialService.save(commercialOffer);
        sendCommercialOfferToCompanyGroupMessageHandler(bot, save);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Tijorat taklifi qabul qilindi* ✅");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Коммерческое предложение принято* ✅");
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);
        UserSession.removeUserState(chatId);
        menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
        UserSession.removeCommercialOffer(chatId);
        basketService.deleteByChatId(chatId);//Agar user tijorat taklifini yuborib bo'lgach savat avtofat tozalanib ketishi kerak bo'lsa
        UserSession.clearMessageChatId(chatId);
    }

    @SneakyThrows
    private void removeMessage(Long chatId, TelegramLongPollingBot bot) {
        Integer messageId = UserSession.getUserMessageId(chatId);
        if (messageId != null) {
            try {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId.toString());
                deleteMessage.setMessageId(messageId);
                bot.execute(deleteMessage);
                UserSession.removeUserMessageId(chatId);
            } catch (TelegramApiRequestException e) {
                if (e.getErrorCode() == 400 && e.getApiResponse().contains("message to delete not found")) {
                    UserSession.removeUserMessageId(chatId);
                } else {
                    throw e;
                }
            }
        }
    }

    @SneakyThrows
    private void removeWarningMessage(Long chatId, TelegramLongPollingBot bot) {
        Integer messageId = UserSession.getUserWarningMessageId(chatId);
        if (messageId != null) {
            try {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId.toString());
                deleteMessage.setMessageId(messageId);
                bot.execute(deleteMessage);
                UserSession.removeUserWarningMessageId(chatId);
            } catch (TelegramApiRequestException e) {
                if (e.getErrorCode() == 400 && e.getApiResponse().contains("message to delete not found")) {
                    UserSession.removeUserMessageId(chatId);
                } else {
                    throw e;
                }
            }
        }
    }

    @SneakyThrows
    private void removeUserWriteMessage(Long chatId, TelegramLongPollingBot bot) {
        Integer messageId = UserSession.getUserWriteMessageId(chatId);
        if (messageId != null) {
            try {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId.toString());
                deleteMessage.setMessageId(messageId);
                bot.execute(deleteMessage);
                UserSession.removeUserMessageId(chatId);
            } catch (TelegramApiRequestException e) {
                if (e.getErrorCode() == 400 && e.getApiResponse().contains("message to delete not found")) {
                    UserSession.removeUserMessageId(chatId);
                } else {
                    throw e;
                }
            }
        }
    }


    @SneakyThrows
    public void removeSpecialMessage(Long chatId, TelegramLongPollingBot bot) {
        List<Integer> specialMessageId = UserSession.getSpecialMessageId(chatId);
        if (specialMessageId == null || specialMessageId.isEmpty())
            return;
        for (Integer messageId : specialMessageId) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(messageId);
            bot.execute(deleteMessage);
        }
        UserSession.removeSpecialMessageIdList(chatId);
    }

    @SneakyThrows
    private void removeAppSpecialMessage(Long chatId, TelegramLongPollingBot bot) {
        List<Integer> appMessageIds = UserSession.getAppMessageIds(chatId);
        if (appMessageIds == null || appMessageIds.isEmpty())
            return;
        for (Integer messageId : appMessageIds) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(messageId);
            bot.execute(deleteMessage);
        }
        UserSession.removeAppMessageIdList(chatId);
    }

    private void sendCommercialOfferToCompanyGroupMessageHandler(TelegramLongPollingBot bot, CommercialOffer commercialOffer) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(GROUP_CHAT_ID);
        sendMessage.setText(
                "*Новое коммерческое предложение \uD83D\uDCCB*\n\n" +
                        "*👤 Ф.И.О:* " + commercialOffer.getFullName() + "\n" +
                        "*📞 Номер телефона:* " + commercialOffer.getPhoneNumber() + "\n" +
                        "*📈 Интересующая услуга: *" + formattedAppServices(commercialOffer.getService()) + "\n\n"
        );
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);

        String jsonBody = createNotionJsonCommercialOffer(commercialOffer);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("https://api.notion.com/v1/pages"))
                .header("Authorization", "Bearer " + "secret_hPZQBlyCfxVGgg1srKYpwZKiZKfQtqACkq0WyhXenXd")
                .header("Content-Type", "application/json")
                .header("Notion-Version", "2022-06-28")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            client.send(request1, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String createNotionJsonCommercialOffer(CommercialOffer offer) {
        return "{"
                + "\"parent\": { \"database_id\": \"" + "cf24a83b587a464389a730fbdf0679f8" + "\" },"
                + "\"properties\": {"
                + "\"Name\": {"
                + "\"title\": [{ \"text\": { \"content\": \"" + escapeJson(offer.getFullName()) + "\" } }]"
                + "},"
                + "\"Type of Service\": {"
                + "\"rich_text\": [{ \"text\": { \"content\": \"" + escapeJson(offer.getService().toString()) + "\" } }]"
                + "},"
                + "\"Phone\": {"
                + "\"phone_number\": \"" + offer.getPhoneNumber() + "\""
                + "}"
                + "}"
                + "}";
    }

    @SneakyThrows
    public void contactMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Biz bilan bog'lanish uchun \uD83D\uDC49 t.me/result_man");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Чтобы связаться с нами \uD83D\uDC49 t.me/result_man");
        bot.execute(sendMessage);
        menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
    }

    @SneakyThrows
    public void ourChannelMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Bizning telegram kanalimiz \uD83D\uDC49 t.me/result_med");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Наш телеграм-канал \uD83D\uDC49 t.me/result_med");
        bot.execute(sendMessage);
        menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
    }
}
