package by.andrey.yarosh.andreyyaroshbot.model;

import lombok.Getter;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Getter
@Entity(name = "users")
public class User {

    @Id
    private Long chatId;

    private String firstName;

    private String lastName;

    private String userName;

    private Timestamp registeredAt;

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setRegisteredAt(Timestamp registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return "\n" +
                " chatId : " + chatId + "\n" +
                " firstName : " + firstName + '\'' + "\n" +
                " lastName : " + lastName + '\'' + "\n" +
                " userName : " + userName + '\'' + "\n" +
                " registeredAt : " + registeredAt;
    }
}
