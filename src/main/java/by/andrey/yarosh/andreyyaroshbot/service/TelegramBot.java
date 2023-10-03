package by.andrey.yarosh.andreyyaroshbot.service;

import by.andrey.yarosh.andreyyaroshbot.config.BotConfig;
import by.andrey.yarosh.andreyyaroshbot.model.User;
import by.andrey.yarosh.andreyyaroshbot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final BotConfig botConfig;
    private final WeatherParser parser;
    private final ExchangeRatesParser exchangeRatesParser;
    static final String HELP_TEXT = "Description of commands : \n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /weather to see weather in Grodno \n\n" +
            "Type /currencies to see exchange rates \n\n" +
            "Type /mydata to see data stored about yourself \n\n" +
            "Type /deletedata to remove your data stored \n\n" +
            "Type /settings to set settings \n\n" +
            "Type /help to see this message again \n\n " +
            "Bot version 1.0 created by Andrey Yarosh";

    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "Error occurred: ";
    public TelegramBot(BotConfig botConfig, UserRepository userRepository, WeatherParser parser, ExchangeRatesParser exchangeRatesParser) {
        this.botConfig = botConfig;
        this.userRepository = userRepository;
        this.parser = parser;
        this.exchangeRatesParser = exchangeRatesParser;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome massage"));
        listOfCommands.add(new BotCommand("/weather", "weather" ));
        listOfCommands.add(new BotCommand("/currencies", "exchange rates" ));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored" ));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data stored" ));
        listOfCommands.add(new BotCommand("/settings", "set your preferences" ));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot" ));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e){
            log.error("Error setting bot command list: " + e.getMessage());
        }

    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            //send message to all users
            if (messageText.contains("/send") && botConfig.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;

                    case "/mydata" :
                        prepareAndSendMessage(chatId, String.valueOf(userDate(chatId)));
                        break;

                    case "/deletedata" :
                        userRepository.deleteById(chatId);
                        prepareAndSendMessage(chatId, "user data has been deleted ");

                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;

                    case "/register":
                        register(chatId);
                        break;

                    case "/weather":
                        prepareAndSendMessage(chatId, parser.weatherParser());
                        break;

                    case "/currencies":
                        prepareAndSendMessage(chatId, exchangeRatesParser.ratesParser());
                        break;

                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not recognized");
                }
            }
        }
        else if (update.hasCallbackQuery()){

            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals(YES_BUTTON)){
                String text = "You pressed YES button";
               executeEditMessageText(chatId,text,messageId);
            }
            else if(callbackData.equals(NO_BUTTON)){
                String text = "You pressed NO button";
                executeEditMessageText(chatId,text,messageId);
            }
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markup.setKeyboard(rowsInLine);

        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void registerUser(Message message) {

        if(userRepository.findById(message.getChatId()).isEmpty()){
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name){

        String answer = EmojiParser.parseToUnicode("Hello , " + name + " :v:");
        log.info("Replied to user " + name);
        prepareAndSendMessage(chatId, answer);
    }

    //keyboard
//    private void sendMessage(long chatId, String textToSend){
//
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId);
//        message.setText(textToSend);
//
//        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//        List<KeyboardRow> keyboardRows = new ArrayList<>();
//
//        KeyboardRow row = new KeyboardRow();
//        row.add("weather");
//        row.add("exchange rates");
//
//        keyboardRows.add(row);
//
//        row = new KeyboardRow();
//
//        row.add("register");
//        row.add("check my data");
//        row.add("delete my data");
//
//        keyboardRows.add(row);
//
//        keyboardMarkup.setKeyboard(keyboardRows);
//
//        message.setReplyMarkup(keyboardMarkup);
//
//        executeMessage(message);
//    }
    private void executeEditMessageText(long chatId, String text, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try{
            execute(message);
        }catch (TelegramApiException e){
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        }
        catch (TelegramApiException e){
            log.error(ERROR_TEXT + e.getMessage());

        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        executeMessage(message);
    }

    private Optional<User> userDate(long chatId){
        return userRepository.findById(chatId);
    }
}
