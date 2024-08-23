package uz.result.resultbot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "basket")
public class Basket {

    @Id
    @GeneratedValue
    private Long id;

    Set<String> service;

    @OneToOne
    @JsonIgnore
    User user;

    @Override
    public String toString() {
        return "Basket{" +
                "id=" + id +
                ", service=" + service +
                '}';
    }
}
