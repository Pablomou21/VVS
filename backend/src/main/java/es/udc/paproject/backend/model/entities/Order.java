package es.udc.paproject.backend.model.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="OrderTable")
public class Order {
    private Long id;
    private int units;
    private Session session;
    private String creditCardNum;
    private LocalDateTime date;
    private boolean delivered;
    private User user;

    public Order() {}

    public Order(int units, Session session, String creditCardNum, LocalDateTime date, User user) {
        this.units = units;
        this.session = session;
        this.creditCardNum = creditCardNum;
        this.date = date.withNano(0);
        this.delivered = false;
        this.user = user;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="sessionId")
    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getCreditCardNum() {
        return creditCardNum;
    }

    public void setCreditCardNum(String creditCardNum) {
        this.creditCardNum = creditCardNum;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="userId")
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Transient
    public BigDecimal getTotalPrice() {
        return session.getPrice().multiply(new BigDecimal(units));
    }
}
