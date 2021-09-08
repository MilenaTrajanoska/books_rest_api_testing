import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.rules.Timeout;
import util.BookDTO;
import util.Constants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
* Class for functional testing of the Books Rest Api
* */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BooksRestApiFunctionalTests {

    /**
     * If a test takes more than 2 seconds to execute, it is unacceptable and should fail FTR22
     */

    @Rule
    public Timeout globalTimeout = org.junit.rules.Timeout.seconds(2);

    private static String encodedAuth;

    /**
     * Parameters for basic authentication
     * */
    private static void setAuthHeader() {
        String username = "user";
        String password = "user123*";
        String auth = username + ":" + password;
        encodedAuth = "Basic " + new String(Base64.encodeBase64(
                auth.getBytes(StandardCharsets.UTF_8)));
    }
    /**
     * Set the base api url which is valid for all test cases
     * */
    @BeforeAll
    public static void setBaseURLForTesting() {
        RestAssured.baseURI = Constants.API_URL;
        setAuthHeader();
    }
    /**
    * Create an instance of the BookDTO class
     * @return BookDTO - BookDTO object
    * */
    private BookDTO buildBookWithoutIsbn() {
        BookDTO book = new BookDTO();
        book.setAuthor("William Shakespeare");
        book.setTitle("Hamlet");
        book.setGenre("Tragedy");
        book.setPrice(13.45);
        return book;
    }

    /**
     *Test for test case: TC1 for test requirement: FTR1
     *Should return all registered books in the books_db (108 in total)
     *Response status code should be 200
     *Content-Type header should be application/json since the data is returned in JSON format
     **/
    @Test
    @Order(1)
    public void shouldReturnAllBooksInDB() {
        //set request specification and get response
        RequestSpecification httpRequest = given();
        Response response = httpRequest.request(Method.GET, "/"); //make a GET request to /api/v1/books/

        //get response body as JSON and map to list of BookDTO
        JsonPath jsonPath = response.getBody().jsonPath();
        List<BookDTO> books = jsonPath.getList("", BookDTO.class); //map the JSON response to List<BookDTO>
        // this line should fail if the correct format of BookDTO object is not returned

        //check that the status code is 200 OK
        assertEquals(response.getStatusCode(), 200);
        //check if three books were returned
        assertEquals(books.size(), Constants.booksInDB);
        //check response content type is application/json
        assertEquals(response.getHeader("Content-Type"), "application/json");

        //check first book contents
        BookDTO book = books.get(0);
        assertEquals(book.getIsbn(), 8781234567891L);
        assertEquals(book.getTitle(), "Chesapeake Blue");
        assertEquals(book.getAuthor(), "Nora Roberts");
        assertEquals(book.getGenre(), "Literature & Fiction");
        assertEquals(book.getPrice(), 25.95);

    }

    /*
     Test for test case: TC2 for test requirement: FTR2
     Should return a single book with isbn equal to 14
     Response status code should be 200
     Content-Type header should be application/json
    */
    @Test
    @Order(2)
    public void shouldReturnAValidBookFromDB() {
        long isbn = 8781234567894L;
        RequestSpecification httpRequest = given();
        Response response = httpRequest.request(Method.GET, "/" + isbn);
        //sends a get request to /api/v1/books/8781234567894

        JsonPath jsonPath = response.getBody().jsonPath();
        //should fail if the api does not return a valid book
        BookDTO bookISBN14 = jsonPath.getObject("",BookDTO.class);

        //check response status code
        assertEquals(response.getStatusCode(), 200);
        //check headers
        assertEquals(response.getHeader("Content-Type"), "application/json");

        //check the payload
        assertEquals(bookISBN14.getIsbn(), isbn);
        assertEquals(bookISBN14.getTitle(), "The Dark Highlander");
        assertEquals(bookISBN14.getGenre(), "Romance");
        assertEquals(bookISBN14.getPrice(), 6.99);
    }

    /*
         Test for test case: TC3 for test requirement: FTR3
         Should return an error message that the book with isbn 500 could not be found
         Response status code should be 404
         Content-Type header should be text/plain;charset=UTF-8
    */
    @Test
    @Order(3)
    public void shouldReturn404ErrorForBookThatDoesNotExistInDB() {
        long id = 500L;
        RequestSpecification httpRequest = given();
        Response response = httpRequest.request(Method.GET, "/" + id);

        assertEquals(response.getStatusCode(), 404);
        assertEquals(response.getHeader("Content-Type"), "text/plain;charset=UTF-8");
        String errorMessage = response.getBody().asString();
        assertEquals(errorMessage, "Book with isbn: 500 could not be found");
    }

    /*
         Test for test case: TC4 for test requirement: FTR3
         Should return an error message bad request when trying to access url with null isbn
         Response status code should be 400
         Content-Type header should be application/json
    */
    @Test
    @Order(4)
    public void shouldReturn404ErrorForNullIsbn() {
        RequestSpecification httpRequest = given();
        Response response = httpRequest.request(Method.GET, "/" + null);

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
         Test for test case: TC5 for test requirement: FTR4
         Should return an error message for an unauthorized resource creation
         Response status code should be 401
         Content-Type header should be application/json
    */
    @Test
    @Order(5)
    public void shouldNotCreateABookFromUnauthorizedUser() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(123L);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");

        Response response = httpRequest.request(Method.POST, "/");
        JsonPath jsonPath = response.getBody().jsonPath();
        String message = jsonPath.getObject("message",String.class);

        assertEquals(response.getStatusCode(), 401);
        assertEquals(message, "Unauthorized");
        assertEquals(response.getHeader("Content-Type"), "application/json");

    }

    /*
         Test for test case: TC6 for test requirement: FTR4
         Should return an error message for Unsupported Media Type
         Response status code should be 415
         Content-Type header should be application/json
    */
    @Test
    @Order(6)
    public void shouldNotCreateABookWhenMissingContentTypeHeader() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(123L);

        RequestSpecification httpRequest = given();
        httpRequest.header("Authorization", encodedAuth);
        httpRequest.body(bookToCreate);

        Response response = httpRequest.request(Method.POST, "/");
        JsonPath jsonPath = response.getBody().jsonPath();
        String message = jsonPath.getObject("message",String.class);

        assertEquals(response.getStatusCode(), 415);
        assertEquals(message, "Content type 'text/plain;charset=ISO-8859-1' not supported");
        assertEquals(response.getHeader("Content-Type"), "application/json");

    }
    /*
             Test for test case: TC7 for test requirement: FTR5
             Should create a valid book in the database
             Response status code should be 201 created
             Content-Type header should be application/json
    */
    @Test
    @Order(7)
    public void shouldCreateAValidBookInTheDB() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(7781234567891L);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.POST, "/");
        JsonPath jsonPath = response.getBody().jsonPath();
        BookDTO savedBook = jsonPath.getObject("",BookDTO.class);

        assertEquals(response.getStatusCode(), 201);
        assertEquals(response.getHeader("Content-Type"), "application/json");

        assertEquals(savedBook.getIsbn(), 7781234567891L);
        assertEquals(savedBook.getTitle(), "Hamlet");
        assertEquals(savedBook.getAuthor(), "William Shakespeare");
        Constants.bookID = savedBook.getIsbn();
    }

    /*
             Test for test case: TC8 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 400 bad request
             Content-Type header should be application/json
    */
    @Test
    @Order(8)
    public void shouldNotCreateBookWithNullIsbn() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(null);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.POST, "/");

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
             Test for test case: TC9 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 400 bad request
             Content-Type header should be application/json
    */
    @Test
    @Order(9)
    public void shouldNotCreateBookWithAnEmptyAuthorName() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(1L);
        bookToCreate.setAuthor("");

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.POST, "/");

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
             Test for test case: TC10 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 400 bad request
             Content-Type header should be application/json
    */
    @Test
    @Order(10)
    public void shouldNotCreateBookWithATitleWithLessThan5Letters() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(2L);
        bookToCreate.setTitle("Abc");

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.POST, "/");

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
             Test for test case: TC11 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 400 bad request
             Content-Type header should be application/json
    */
    @Test
    @Order(11)
    public void shouldNotCreateBookWithANegativeValueForPrice() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(2L);
        bookToCreate.setPrice(-1.0);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.POST, "/");

        Constants.booksToDelete.add(2L);
        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
             Test for test case: TC12 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 403 forbidden
             Content-Type header should be application/json
    */
    @Test
    @Order(12)
    public void shouldNotCreateABookWithAnIsbnThatAlreadyExists() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(8781234567891L);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.POST, "/");

        String message = response.getBody().asString();

        assertEquals(response.getStatusCode(), 403);
        assertEquals(response.getHeader("Content-Type"), "text/plain;charset=UTF-8");
        assertEquals(message, "Book with isbn: 8781234567891 already exists");

    }

    /*
             Test for test case: TC13 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 400 bad request
             Content-Type header should be application/json
    */
    @Test
    @Order(13)
    public void shouldReturnBadRequestWhenFieldIsWrongType() throws JSONException {
        JSONObject obj1 = new JSONObject();
        obj1.put("isbn", 55);
        obj1.put("author", 123455);
        obj1.put("title", "Bird Box");
        obj1.put("genre", "Horror");
        obj1.put("price", 3.5);

        RequestSpecification httpRequest = given();
        httpRequest.body(obj1.toString());
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);
        Constants.booksToDelete.add(55L);

        Response response = httpRequest.request(Method.POST, "/");
        System.out.println(response.getBody().asString());
        assertEquals(response.getStatusCode(), 400); //unsupported media type
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
             Test for test case: TC14 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 400 bad request
             Content-Type header should be application/json
    */
    @Test
    @Order(14)
    public void shouldNotCreateObjectInDBWithMissingFieldForPrice() throws JSONException {
        JSONObject obj1 = new JSONObject();
        obj1.put("isbn", 55);
        obj1.put("title", "Title");
        obj1.put("author", "Author");
        obj1.put("genre", "Horror");

        RequestSpecification httpRequest = given();
        httpRequest.body(obj1.toString());
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.POST, "/");
        System.out.println(response.getBody().asString());
        assertEquals(response.getStatusCode(), 400); //unsupported media type
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
             Test for test case: TC15 for test requirement: FTR6
             Should not create invalid book in the database
             Response status code should be 400 bad request
             Content-Type header should be application/json
    */
    @Test
    @Order(15)
    public void shouldIgnoreRedundantFieldsWhenCreatingObject() throws JSONException {
        JSONObject obj1 = new JSONObject();
        obj1.put("isbn", 105);
        obj1.put("title", "Title");
        obj1.put("author", "Author");
        obj1.put("genre", "Horror");
        obj1.put("price", 23.3);
        obj1.put("random", "Random Field");

        RequestSpecification httpRequest = given();
        httpRequest.body(obj1.toString());
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);
        Constants.booksToDelete.add(105L);

        Response response = httpRequest.request(Method.POST, "/");
        System.out.println(response.getBody().asString());
        JsonPath jsonPath = response.getBody().jsonPath();
        BookDTO savedBook = jsonPath.getObject("",BookDTO.class);

        assertEquals(response.getStatusCode(), 201); //unsupported media type
        assertEquals(response.getHeader("Content-Type"), "application/json");
        assertEquals(savedBook.getIsbn(), 105L);
        assertEquals(savedBook.getPrice(), 23.3);
        assertEquals(savedBook.getAuthor(), "Author");
        assertEquals(savedBook.getTitle(), "Title");
        assertEquals(savedBook.getGenre(), "Horror");
    }

    /*
             Test for test case: TC16 for test requirement: FTR7
             Should not update a book from unauthorized user
             Response status code should be 401 unauthorized
             Content-Type header should be application/json
    */
    @Test
    @Order(16)
    public void shouldNotUpdateABookFromUnauthorizedUser() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(18L);
        bookToCreate.setPrice(20.15);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Content-Type", "application/json");

        Response response = httpRequest.request(Method.PUT, "/18");
        JsonPath jsonPath = response.getBody().jsonPath();
        String message = jsonPath.getObject("message",String.class);

        assertEquals(response.getStatusCode(), 401);
        assertEquals(message, "Unauthorized");
        assertEquals(response.getHeader("Content-Type"), "application/json");

    }

    /*
             Test for test case: TC17 for test requirement: FTR7
             Should not update a book with unsupported media type
             Response status code should be 415 unauthorized
             Content-Type header should be application/json
    */
    @Test
    @Order(17)
    public void shouldNotUpdateBookWithMissingContentTypeHeader() {
        BookDTO bookToCreate = buildBookWithoutIsbn();
        bookToCreate.setIsbn(18L);
        bookToCreate.setPrice(20.15);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToCreate);
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.PUT, "/18");
        JsonPath jsonPath = response.getBody().jsonPath();
        String message = jsonPath.getObject("message",String.class);

        assertEquals(response.getStatusCode(), 415);
        assertEquals(message, "Content type 'text/plain;charset=ISO-8859-1' not supported");
        assertEquals(response.getHeader("Content-Type"), "application/json");

    }

    /*
    Test for test case: TC18 for test requirement: FTR8
    Should update existing book with isbn equal to 9781234567108 with valid information
    Response status code should be 200 OK
    Content-Type header should be application/json
    */
    @Test
    @Order(18)
    public void shouldUpdateExistingBookWithValidInformation() {
        BookDTO bookToUpdate = buildBookWithoutIsbn();
        bookToUpdate.setIsbn(9781234567108L);
        bookToUpdate.setPrice(20.15);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToUpdate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.PUT, "/" + 9781234567108L);
        JsonPath jsonPath = response.getBody().jsonPath();
        BookDTO updatedBook = jsonPath.getObject("",BookDTO.class);

        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getHeader("Content-Type"), "application/json");

        assertEquals(updatedBook.getTitle(), "Hamlet");
        assertEquals(updatedBook.getAuthor(), "William Shakespeare");
        assertEquals(updatedBook.getPrice(), 20.15);
        assertEquals(updatedBook.getIsbn(), 9781234567108L);

    }

    /*
   Test for test case: TC19 for test requirement: FTR9
   Should not update existing book with isbn equal to 7781234567891 with invalid information
   Response status code should be 400 bad request
   Content-Type header should be application/json
   */
    @Test
    @Order(19)
    public void shouldNotUpdateExistingBookWithNullIsbn() {
        BookDTO bookToUpdate = buildBookWithoutIsbn();
        bookToUpdate.setIsbn(null);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToUpdate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.PUT, "/" + Constants.bookID);

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
   Test for test case: TC20 for test requirement: FTR10
   Should not create new book when updating book with isbn that does not exist
   Response status code should be 404 book not found
   Content-Type header should be text/plain;charset=UTF-8
   */
    @Test
    @Order(20)
    public void shouldNotCreateNewBookWhenUpdatingIsbnThatDoesNotExist() {
        BookDTO bookToUpdate = new BookDTO();
        bookToUpdate.setIsbn(6681234567891L);
        bookToUpdate.setAuthor("Josh Malerman");
        bookToUpdate.setTitle("Bird Box");
        bookToUpdate.setGenre("Horror");
        bookToUpdate.setPrice(3.5);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToUpdate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.PUT, "/" + 6681234567891L);

        assertEquals(response.getStatusCode(), 404);
        assertEquals(response.getHeader("Content-Type"), "text/plain;charset=UTF-8");

        String errorMessage = response.getBody().asString();
        assertEquals(errorMessage, "Book with isbn: 6681234567891 could not be found");
    }

    /*
   Test for test case: TC21 for test requirement: FTR10
   Should return bad request error when updating book with null isbn
   Response status code should be 400 bad request
   Content-Type header should be application/json
   */
    @Test
    @Order(21)
    public void shouldReturnErrorWhenUpdatingNullIsbn() {
        BookDTO bookToUpdate = new BookDTO();
        bookToUpdate.setIsbn(6681234567891L);
        bookToUpdate.setAuthor("Josh Malerman");
        bookToUpdate.setTitle("Bird Box");
        bookToUpdate.setGenre("Horror");
        bookToUpdate.setPrice(3.5);

        RequestSpecification httpRequest = given();
        httpRequest.body(bookToUpdate);
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.PUT, "/" + null);

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");

    }


    /*
   Test for test case: TC22 for test requirement: FTR11
   Should not delete book unauthorized
   Response status code should be 401 unauthorized
   Content-Type header should be application/json
   */
    @Test
    @Order(22)
    public void shouldNotDeleteABookUnauthorized() {
        RequestSpecification httpRequest = given();
        Response response = httpRequest.request(Method.DELETE, "/7781234567891");

        JsonPath jsonPath = response.getBody().jsonPath();
        String message = jsonPath.getObject("message",String.class);

        assertEquals(message, "Unauthorized");
        assertEquals(response.getStatusCode(), 401);
        assertEquals(response.getHeader("Content-Type"), "application/json");

    }

    /*
   Test for test case: TC23 for test requirement: FTR12
   Should delete existing book from database
   Response status code should be 200 ok
   Content-Type header should be application/json
   Message should be: Book deleted successfully
   */
    @Test
    @Order(23)
    public void shouldDeleteABookWithAnExistingIsbn() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Authorization", encodedAuth);
        Response response = httpRequest.request(Method.DELETE, "/" + Constants.bookID);

        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getHeader("Content-Type"), "application/json");

        JsonPath jsonPath = response.getBody().jsonPath();
        String message = jsonPath.getObject("message",String.class);
        assertEquals(message, "Book deleted successfully");
    }

    /*
   Test for test case: TC24 for test requirement: FTR13
   Should not delete book that doesn't exist
   Response status code should be 404 not found
   Content-Type header should be text/plain;charset=UTF-8
   Message should be: Book with isbn: {isbn} not found
   */
    @Test
    @Order(24)
    public void shouldNotDeleteBookWhenIsbnDoesNotExist() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.DELETE, "/" + 600);

        assertEquals(response.getStatusCode(), 404);
        assertEquals(response.getHeader("Content-Type"), "text/plain;charset=UTF-8");

        String errorMessage = response.getBody().asString();
        assertEquals(errorMessage, "Book with isbn: 600 could not be found");
    }

    /*
   Test for test case: TC25 for test requirement: FTR13
   Should not delete book when passed null isbn
   Response status code should be 400 bad request
   Content-Type header should be application/json
   */
    @Test
    @Order(18)
    public void shouldReturnBadRequestErrorWhenDeletingBookWithNullIsbn() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Authorization", encodedAuth);

        Response response = httpRequest.request(Method.DELETE, "/" + null);

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
   Test for test case: TC26 for test requirement: FTR14
   Should return method not supported error when calling endpoint with unsupported HTTP method
   Status code should be 405 method not allowed
   Content-Type should be application/json
   */
    @Test
    @Order(26)
    public void shouldReturnBadRequestWhenSettingUnsupportedMethodForBaseEndpoint() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Authorization", encodedAuth);
        Response response = httpRequest.request(Method.DELETE, "/");

        assertEquals(response.getStatusCode(), 405);

        assertEquals(response.getHeader("Content-Type"), "application/json");
    }


    /*
   Test for test case: TC27 for test requirement: FTR15
   Should return method not supported error when calling endpoint with unsupported HTTP method
   Status code should be 405 method not allowed
   Content-Type should be application/json
   */
    @Test
    @Order(27)
    public void shouldReturnBadRequestWhenSettingUnsupportedMethodForResourceEndpoint() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Authorization", encodedAuth);
        Response response = httpRequest.request(Method.POST, "/7781234567891");

        assertEquals(response.getStatusCode(), 405);

        assertEquals(response.getHeader("Content-Type"), "application/json");
    }

    /*
     Test for test case: TC28 for test requirement: FTR16
     Should return method not acceptable error when calling endpoint with unsupported HTTP header
     Status code should be 406 not acceptable
     Content-Type should be application/json
     */
    @Test
    @Order(28)
    public void shouldNotAcceptJsonWhenHeaderIsTextHtml() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Accept", "text/html");
        Response response = httpRequest.request(Method.GET, "/");

        assertEquals(response.getStatusCode(), 406);

        assertEquals(response.getHeader("Content-Type"), "text/html;charset=UTF-8");
    }

    /*
     Test for test case: TC29 for test requirement: FTR17
     Should return method not acceptable error when calling endpoint with unsupported HTTP header
     Status code should be 406 not acceptable
     Content-Type should be application/json
     */
    @Test
    @Order(29)
    public void shouldNotAcceptJsonWhenHeaderIsXml() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Accept", "application/xml");
        Response response = httpRequest.request(Method.GET, "/8781234567891");

        assertEquals(response.getStatusCode(), 406);

        assertEquals(response.getHeader("Content-Length"), "0");
    }

    /*
     Test for test case: TC30 for test requirement: FTR18
     Should test parallel creation of the same resource
     Status code should be 201 created once
     and 403 forbidden 6 times
     */
    @Test
    @Order(30)
    public void shouldCreateResourceOnlyOnceWhenExecutingInParallel() {
        ExecutorService executorService = Executors.newFixedThreadPool(7);
        BookDTO body = new BookDTO();
        body.setIsbn(2L);
        body.setAuthor("Josh Malerman");
        body.setTitle("Bird Box");
        body.setGenre("Horror");
        body.setPrice(3.5);
        Constants.booksToDelete.add(2L);

        List<Integer> statusCodes = new ArrayList<>();
        for (int i = 0; i < 7; i++){
            executorService.execute(() -> {
                try {
                    int status = given().
                            contentType("application/json").
                    header("Authorization", encodedAuth).
                    body(body).
                            when().
                            post(Constants.API_URL).getStatusCode();
                    statusCodes.add(status);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }

        // Wait for all the threads to gracefully shutdown

        try {
            boolean b = executorService.awaitTermination(2, TimeUnit.SECONDS);
            statusCodes.forEach(System.out::println);
            assertEquals(statusCodes.stream().filter(s -> s==201).count(), 1);
            assertEquals(statusCodes.stream().filter(s->s==403).count(), 6);

        } catch (InterruptedException ie) {
            System.out.println("Shutdown interrupted. Will try to shutdown again." + ie);
            executorService.shutdownNow();
        }
    }

    /*
     Test for test case: TC31 for test requirement: FTR18
     Should test parallel update of the same resource
     Status code should be 200
     The resource should have the values of the last update
     */
    @Test
    @Order(31)
    public void shouldKeepTheLastUpdateOfTheResourceWhenExecutingInParallel() {
        ExecutorService executorService = Executors.newFixedThreadPool(7);
        Long isbn = 8781234567892L;
        List<BookDTO> bookList = List.of(
                new BookDTO(isbn, "Hamlet", "William Shakespeare", "Tragedy", 15.3),
                new BookDTO(isbn, "Bird Box", "Josh Malerman", "Horror", 11.3),
                new BookDTO(isbn, "The Fault in Our Stars", "John Green", "Sth", 23.11),
                new BookDTO(isbn, "The Martian", "Andy Weir", "Sci-Fi", 12.2),
                new BookDTO(isbn, "The Notebook", "Nicholas Sparks", "Novel", 4.15),
                new BookDTO(isbn, "Death of a Salesman", "Arthur Miller", "Play", 5.2),
                new BookDTO(isbn, "Les Miserables", "Victor Hugo", "Drama", 14.55)
        );

        List<Integer> statusCodes = new ArrayList<>();
        for (BookDTO book : bookList){

            executorService.execute(() -> {
                try {
                    int status = given().
                            contentType("application/json").
                            header("Authorization", encodedAuth).
                            body(book).
                            when().
                            put(Constants.API_URL + "/" + isbn).getStatusCode();
                    statusCodes.add(status);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }

        // Wait for all the threads to gracefully shutdown

        try {
            boolean b = executorService.awaitTermination(2, TimeUnit.SECONDS);
            statusCodes.forEach(System.out::println);
            assertEquals(statusCodes.stream().filter(s -> s==200).count(), 7);

            BookDTO result = given()
                    .when()
                    .get(Constants.API_URL + "/"
                            + isbn)
                    .jsonPath()
                    .getObject("", BookDTO.class);

            BookDTO refBook = bookList.stream().filter(book -> book.getTitle().equals(result.getTitle())).findFirst().get();
            System.out.println(result);
            System.out.println(refBook);
            assertEquals(result.getIsbn(), refBook.getIsbn());
            assertEquals(result.getGenre(), refBook.getGenre());
            assertEquals(result.getTitle(), refBook.getTitle());
            assertEquals(result.getAuthor(), refBook.getAuthor());
            assertEquals(result.getPrice(), refBook.getPrice());

        } catch (InterruptedException ie) {
            System.out.println("Shutdown interrupted. Will try to shutdown again." + ie);
            executorService.shutdownNow();
        }
    }

    /*
     Test for test case: TC32 for test requirement: FTR18
     Should delete resource only once in parallel
     Status code should be 200 once and 404 not found 6 times
     */
    @Test
    @Order(32)
    public void shouldDeleteResourceOnlyOnceWhenExecutingInParallel() {
        ExecutorService executorService = Executors.newFixedThreadPool(7);
        BookDTO book = buildBookWithoutIsbn();
        book.setIsbn(5L);
        given().
                contentType("application/json").
                header("Authorization", encodedAuth).
                body(book).
                when().
                post(Constants.API_URL);

        List<Integer> statusCodes = new ArrayList<>();
        for (int i=0; i<7; i++){
            executorService.execute(() -> {
                try {
                    int status = given().
                            header("Authorization", encodedAuth).
                            when().
                            delete(Constants.API_URL + "/" + 5L).getStatusCode();
                    statusCodes.add(status);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }

        // Wait for all the threads to gracefully shutdown

        try {
            boolean b = executorService.awaitTermination(2, TimeUnit.SECONDS);
            statusCodes.forEach(System.out::println);
            assertEquals(statusCodes.stream().filter(s -> s==200).count(), 1);
            assertEquals(statusCodes.stream().filter(s->s==404).count(), 6);
        } catch (InterruptedException ie) {
            System.out.println("Shutdown interrupted. Will try to shutdown again." + ie);
            executorService.shutdownNow();
        }
    }


    @AfterAll
    public static void restoreDBOriginalState() {
        RequestSpecification httpRequest = given();
        httpRequest.header("Authorization", encodedAuth);
        for(Long isbn : Constants.booksToDelete){
            httpRequest.request(Method.DELETE, "/" + isbn);
        }
    }
}
