package uz.result.resultbot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.result.resultbot.model.Language;
import uz.result.resultbot.model.User;
import uz.result.resultbot.model.UserState;
import uz.result.resultbot.repository.UserRepository;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public void save(User user) {
        Optional<User> optionalUser = userRepository.findByChatId(user.getChatId());
        if (optionalUser.isEmpty()) {
            userRepository.save(user);
            return;
        }
        logger.warn("User is already saved with CHAT_ID: {}", optionalUser.get().getChatId());
        throw new RuntimeException("User is already saved with this chatId");
    }

    public User findByChatId(Long chatId) {
        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> {
                    logger.warn("User is not found with this CHAT_ID: {}", chatId);
                    return new RuntimeException("User is not found with this chatId: " + chatId);
                });
    }

    public UserState getUserState(Long chatId) {
        return userRepository.findUserStateByChatId(chatId).orElse(UserState.DEFAULT);
    }

    public Future<Language> getLanguage(Long chatId) {
        return executorService.submit(userRepository.findByChatId(chatId)
                .orElseThrow(() -> {
                    logger.warn("User is not found with CHAT_ID: {}", chatId);
                    return new RuntimeException("User is not found with this chatId");
                })::getLanguage);
    }

    public UserState updateUserState(Long chatId, UserState state) {
        if (existsByChatId(chatId)) {
            userRepository.updateState(chatId, state.name());
            return state;
        }
        return getUserState(chatId);
    }

    public boolean existsByChatId(Long chatId) {
        return userRepository.existsByChatId(chatId);
    }

    public void changeLanguage(Long chatId, String language) {
        if (existsByChatId(chatId))
            userRepository.updateLanguage(chatId, language);
        else {
            logger.warn("User is not found with CHAT_ID: {}", chatId);
            throw new RuntimeException("User is not found with this chatId: " + chatId);
        }
    }
}
