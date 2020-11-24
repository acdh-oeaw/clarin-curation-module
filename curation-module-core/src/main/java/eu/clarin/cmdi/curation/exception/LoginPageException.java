package eu.clarin.cmdi.curation.exception;

/**
 * The link redirects to a page which is in the login pages list: https://raw.githubusercontent.com/clarin-eric/login-pages/master/list.txt
 */
public class LoginPageException extends Exception {
    public LoginPageException(String message) {
        super(message);
    }
}
