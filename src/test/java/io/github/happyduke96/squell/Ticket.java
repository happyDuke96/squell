package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.GeneratedValue;
import io.github.happyduke96.squell.sql.annotation.Id;

@Entity("tickets")
public interface Ticket {

    @Id
    @GeneratedValue
    long id();

    String label();
}
