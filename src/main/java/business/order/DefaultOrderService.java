package business.order;

import api.ApiException;
import business.book.Book;
import business.book.BookDao;
import business.cart.ShoppingCart;
import business.customer.CustomerForm;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.util.Date;

public class DefaultOrderService implements OrderService {

	private BookDao bookDao;

	public void setBookDao(BookDao bookDao) {
		this.bookDao = bookDao;
	}

	@Override
	public OrderDetails getOrderDetails(long orderId) {
		// NOTE: THIS METHOD PROVIDED NEXT PROJECT
		return null;
	}

	@Override
    public long placeOrder(CustomerForm customerForm, ShoppingCart cart) {

		validateCustomer(customerForm);
		validateCart(cart);

		// NOTE: MORE CODE PROVIDED NEXT PROJECT

		return -1;
	}


	private void validateCustomer(CustomerForm customerForm) {

    	String name = customerForm.getName();
		String address = customerForm.getAddress();
		String phone = customerForm.getPhone();
		String email = customerForm.getEmail();
		String ccNumber = customerForm.getCcNumber();

		if (name == null || name.equals("") || name.length() > 45) {
			throw new ApiException.ValidationFailure("name", "Invalid name field");
		}

		if (address == null || address.equals("") || address.length() < 4 || address.length() > 45) {
			throw new ApiException.ValidationFailure("Address","Invalid address field");
		}

		if(phone == null || phone.equals("") || phone.replaceAll("\\D", "").length() != 10){
			throw new ApiException.ValidationFailure("phone","Invalid phone field");
		}

		if(email == null || email.equals("") || email.contains(" ") || !(email.contains("@")) || (email.charAt(email.length() - 1) == '.')){
			throw new ApiException.ValidationFailure("email","Invalid email field");
		}

		if(ccNumber.equals("") || ccNumber.replaceAll("[\\s\\-]", "").length()<14 || ccNumber.replaceAll("[\\s\\-]", "").length()>16){
			throw new ApiException.ValidationFailure("ccNumber","Invalid ccNumber field");
		}

		if (expiryDateIsInvalid(customerForm.getCcExpiryMonth(), customerForm.getCcExpiryYear())) {
			throw new ApiException.ValidationFailure("expiry","Invalid expiry date");

		}
	}

	private boolean expiryDateIsInvalid(String ccExpiryMonth, String ccExpiryYear) {

		if(ccExpiryMonth == null || ccExpiryMonth.equals("") || ccExpiryYear == null || ccExpiryYear.equals("")){
			return true;
		}

		if(Integer.parseInt(ccExpiryMonth) <= 0 || Integer.parseInt(ccExpiryMonth) > 12){
			return true;
		}
		YearMonth currentDate = YearMonth.now(); // 2022-11
		int currentYear = currentDate.get(ChronoField.YEAR);
		int currentMonth = currentDate.get(ChronoField.MONTH_OF_YEAR);

		if(currentYear > Integer.parseInt(ccExpiryYear)){
			return true;
		}
		else if(currentYear == Integer.parseInt(ccExpiryYear)){
			return currentMonth > Integer.parseInt(ccExpiryMonth);
		}
		return false;

	}

	private void validateCart(ShoppingCart cart) {

		if (cart.getItems().size() <= 0) {
			throw new ApiException.ValidationFailure("Cart is empty.");
		}

		cart.getItems().forEach(item-> {
			if (item.getQuantity() < 0 || item.getQuantity() > 99) {
				throw new ApiException.ValidationFailure("Invalid quantity");
			}
			Book databaseBook = bookDao.findByBookId(item.getBookId());
			if(item.getBookForm().getPrice() != databaseBook.getPrice()){
				throw new ApiException.ValidationFailure("Some books in your cart have their price changed");
			}
			if(item.getBookForm().getCategoryId() != databaseBook.getCategoryId() ){
				throw new ApiException.ValidationFailure("Some books in your cart are of invalid category");
			}
		});
	}

}
