package business.order;

import api.ApiException;
import business.BookstoreDbException;
import business.JdbcUtils;
import business.book.Book;
import business.book.BookDao;
import business.cart.ShoppingCart;
import business.cart.ShoppingCartItem;
import business.customer.Customer;
import business.customer.CustomerForm;
import business.customer.CustomerDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Date;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DefaultOrderService implements OrderService {

	private BookDao bookDao;
	private OrderDao orderDao;
	private LineItemDao lineItemDao;
	private CustomerDao customerDao;

	public void setBookDao(BookDao bookDao) {
		this.bookDao = bookDao;
	}

	public void setOrderDao(OrderDao orderDao) {
		this.orderDao = orderDao;
	}

	public void setLineItemDao(LineItemDao lineItemDao) {
		this.lineItemDao = lineItemDao;
	}

	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}


	@Override
	public OrderDetails getOrderDetails(long orderId) {
		Order order = orderDao.findByOrderId(orderId);
		Customer customer = customerDao.findByCustomerId(order.getCustomerId());
		List<LineItem> lineItems = lineItemDao.findByOrderId(orderId);
		List<Book> books = lineItems
				.stream()
				.map(lineItem -> bookDao.findByBookId(lineItem.getBookId()))
				.collect(Collectors.toList());
		return new OrderDetails(order, customer, lineItems, books);
	}

	private Date getDate(String monthString, String yearString) {
		YearMonth expiry = YearMonth.of(Integer.parseInt(yearString), Integer.parseInt(monthString));
		return Date.from(expiry.atEndOfMonth().atTime(23,59,59).toInstant(ZoneOffset.UTC));
	}

	@Override
    public long placeOrder(CustomerForm customerForm, ShoppingCart cart) {

		validateCustomer(customerForm);
		validateCart(cart);
		try (Connection connection = JdbcUtils.getConnection()) {
			Date date = getDate(
					customerForm.getCcExpiryMonth(),
					customerForm.getCcExpiryYear());
			return performPlaceOrderTransaction(
					customerForm.getName(),
					customerForm.getAddress(),
					customerForm.getPhone(),
					customerForm.getEmail(),
					customerForm.getCcNumber(),
					date, cart, connection);
		} catch (SQLException e) {
			throw new BookstoreDbException("Error during close connection for customer order", e);
		}
	}

	private long performPlaceOrderTransaction(
			String name, String address, String phone,
			String email, String ccNumber, Date date,
			ShoppingCart cart, Connection connection) {
		try {
			connection.setAutoCommit(false);
			long customerId = customerDao.create(
					connection, name, address, phone, email,
					ccNumber, date);
			long customerOrderId = orderDao.create(
					connection,
					cart.getComputedSubtotal() + cart.getSurcharge(),
					generateConfirmationNumber(), customerId);
			for (ShoppingCartItem item : cart.getItems()) {
				lineItemDao.create(connection, customerOrderId, item.getBookId(), item.getQuantity());
			}
			connection.commit();
			return customerOrderId;
		} catch (Exception e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new BookstoreDbException("Failed to roll back transaction", e1);
			}
			return 0;
		}
	}

	private int generateConfirmationNumber() {
		Random random = new Random();
		return random.nextInt(999999999);
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
