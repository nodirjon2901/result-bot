package uz.result.resultbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.result.resultbot.bot.UserSession;
import uz.result.resultbot.model.Application;
import uz.result.resultbot.model.User;
import uz.result.resultbot.repository.ApplicationRepository;

import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public Application save(Application application) {
        Optional<Application> optionalApplication = applicationRepository.findByUserId(application.getUser().getId());
        if (optionalApplication.isPresent()) {
            application.setId(optionalApplication.get().getId());
            return update(application);
        } else {
            return applicationRepository.save(application);
        }
    }

    public Application update(Application application) {
        Application oldApp = applicationRepository.findById(application.getId())
                .orElseThrow(() -> new RuntimeException("Application is not found by id: " + application.getId()));
        oldApp.setId(oldApp.getId());
        oldApp.setService(application.getService());
        oldApp.setFullName(application.getFullName());
        oldApp.setPhoneNumber(application.getPhoneNumber());
        oldApp.setUser(application.getUser());
        return applicationRepository.save(oldApp);
    }

    public void updateFullName(String fullName, Long chatId) {
        Application application = UserSession.getApplication(chatId);
        application.setFullName(fullName);
        UserSession.updateUserApplication(chatId, application);
    }

    public void updatePhoneNum(String phoneNum, Long chatId) {
        Application application = UserSession.getApplication(chatId);
        application.setPhoneNumber(phoneNum);
        UserSession.updateUserApplication(chatId, application);
    }

    public void updateService(String service, Long chatId) {
        Application application = UserSession.getApplication(chatId);
        if (application.getService() == null) {
            application.setService(new HashSet<>());
        }
        application.getService().add(service);
        UserSession.updateUserApplication(chatId, application);
    }

    public void clearServices(Long chatId) {
        Application application = UserSession.getApplication(chatId);
        application.setService(new HashSet<>());
        UserSession.updateUserApplication(chatId, application);
    }

    public Application setUserInApplication(Long chatId, User user) {
        Application application = UserSession.getApplication(chatId);
        application.setUser(user);
        UserSession.updateUserApplication(chatId, application);
        return UserSession.getApplication(chatId);
    }
}
