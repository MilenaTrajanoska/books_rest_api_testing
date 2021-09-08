package util;

import java.io.Serializable;

public class BookDTO implements Serializable {

    @Override
    public String toString() {
        return "{" +
                "isbn: " + isbn +
                ", title: '" + title + '\'' +
                ", author: '" + author + '\'' +
                ", genre: '" + genre + '\'' +
                ", price: " + price +
                '}';
    }

    private Long isbn;
    private String title;
    private String author;
    private String genre;
    private double price;

    public BookDTO() {

    }

    public BookDTO(Long isbn, String title, String author, String genre, double price) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.price = price;
    }

    public Long getIsbn() {
        return isbn;
    }

    public void setIsbn(Long isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }

    public double getPrice() {
        return price;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setPrice(double price) {
        this.price = price;
    }

}
