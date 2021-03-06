package servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.CategoryDAO;
import dao.CustomerDAO;
import dao.OrderDAO;
import dao.OrderedProductDAO;
import dao.ProductDAO;
import entities.Cart;
import entities.OrderedProduct;
import entities.Product;
import ultilities.Constants_Value;

/**
 * Servlet implementation class AddToCartController
 */
@WebServlet(urlPatterns =
{ Constants_Value.CART_ADD_URL, Constants_Value.CART_CHECKOUT_URL, Constants_Value.CART_DELETE_URL,
		Constants_Value.CART_URL })
public class CartController extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	Cart cart = new Cart();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CartController()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init() throws ServletException
	{
		// TODO Auto-generated method stub
		super.init();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String action = request.getServletPath();
		switch (action)
		{
		case Constants_Value.CART_URL:
			// get all categories
			request.setAttribute("categoryList", CategoryDAO.getAll());
			doGet_GetCart(request, response);
			request.getRequestDispatcher(Constants_Value.FILE_HOME_CHECKOUT_URL).forward(request, response);
			break;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String action = request.getServletPath();
		switch (action)
		{
		case Constants_Value.CART_ADD_URL:
			doPost_AddToCart(request, response);
			break;
		case Constants_Value.CART_CHECKOUT_URL:
			String customerName = request.getParameter("customer_name");
			String customerEmail = request.getParameter("customer_email");
			String customerAddress = request.getParameter("customer_address");
			String customerPhone = request.getParameter("customer_phone");
			if (customerName == "" || customerEmail == "" || customerAddress == "" || customerPhone == "")
			{
				request.setAttribute("message", "Thiếu thông tin khách hàng, mời nhập đầy đủ");
				doGet_GetCart(request, response);
				request.getRequestDispatcher(Constants_Value.FILE_HOME_CHECKOUT_URL).forward(request, response);
			}
			else if (request.getSession().getAttribute(Constants_Value.SESSION_CART) == null)
			{
				request.setAttribute("message", "Giỏ hàng trống, không thể đặt hàng được");
				doGet_GetCart(request, response);
				request.getRequestDispatcher(Constants_Value.FILE_HOME_CHECKOUT_URL).forward(request, response);
			}
			else
				doPost_Checkout(request, response);
			break;
		case Constants_Value.CART_DELETE_URL:
			doPost_ItemDeleteFromCart(request, response);
			break;
		}

	}

	protected void doGet_GetCart(HttpServletRequest request, HttpServletResponse response)
	{
		// get session
		if (request.getSession().getAttribute(Constants_Value.SESSION_CART) != null)
		{
			cart = (Cart) request.getSession().getAttribute(Constants_Value.SESSION_CART);
			request.setAttribute("items", cart.getItems()); // get items in cart
			request.setAttribute("sumPrice", cart.getSumPrice()); // get sum price
		}
	}

	protected void doPost_AddToCart(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		HttpSession session = request.getSession();
		// Find session cart, if not exist then create a new one
		if (session.getAttribute(Constants_Value.SESSION_CART) == null)
		{
			session.setAttribute(Constants_Value.SESSION_CART, cart);
		}

		// Get product id and order quantity from form
		int product_id = Integer.parseInt(request.getParameter("product_id"));
		int orderQuantity = Integer.parseInt(request.getParameter("quantity"));

		// Create a new product object from selected id
		Product product = ProductDAO.getProductByID(product_id);

		// cart.setOrderQuantity(orderQuantity);
		cart.addToCart(product, orderQuantity); // add the product to cart

		response.sendRedirect(request.getContextPath() + Constants_Value.HOME_INDEX_URL);
	}

	protected void doPost_Checkout(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		// get parameters
		String customerName = request.getParameter("customer_name");
		String customerEmail = request.getParameter("customer_email");
		String customerAddress = request.getParameter("customer_address");
		String customerPhone = request.getParameter("customer_phone");
		String customerSessionID = UUID.randomUUID().toString();

		try
		{
			// if customer order 1 or more product, then proceed
			if (cart.getItems().size() > 0)
			{
				// abstracting product in store quantity
				// and check if still have that product left in store
				boolean flagProductStillInStore = true;
				for (int i = 0; i < cart.getItems().size(); i++)
				{
					if (OrderedProduct.checkInStoreQuantity(cart.getItems().get(i)) == false)
					{
						flagProductStillInStore = false;
						break;
					}
				}
				// if product still in store, proceed
				if (flagProductStillInStore)
				{
					// abstracting in store quantity
					for (int i = 0; i < cart.getItems().size(); i++)
					{
						int newQuantity = ProductDAO.getQuantity(cart.getItems().get(i).getId())
								- cart.getItems().get(i).getOrderQuantity();
						ProductDAO.setQuantity(cart.getItems().get(i).getId(), newQuantity);
					}
					// add customer to database
					CustomerDAO.add(customerName, customerEmail, customerAddress, customerPhone, customerSessionID);
					// add order to database and link customer session id to order's customer_id
					OrderDAO.add(customerSessionID);
					// add list of ordered products to database
					for (int i = 0; i < cart.getItems().size(); i++)
					{
						OrderedProduct orderedProduct = new OrderedProduct();
						orderedProduct.setProductID(cart.getItems().get(i).getId());
						orderedProduct.setQuantity(cart.getItems().get(i).getOrderQuantity());
						orderedProduct.setOrderID(OrderDAO.getOrderID(customerSessionID));

						OrderedProductDAO.add(orderedProduct);
					}
					// delete session cart
					request.getSession().removeAttribute(Constants_Value.SESSION_CART);
				}
			}
		} catch (ClassNotFoundException | SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.sendRedirect(request.getContextPath() + Constants_Value.HOME_INDEX_URL);
	}

	protected void doPost_ItemDeleteFromCart(HttpServletRequest request, HttpServletResponse response)
			throws IOException
	{
		if (request.getSession().getAttribute(Constants_Value.SESSION_CART) != null)
		{
			cart = (Cart) request.getSession().getAttribute(Constants_Value.SESSION_CART);
		}
		// delete selected product in cart
		int product_id = Integer.parseInt(request.getParameter("product_id"));
		cart.deleteItem(product_id);

		// redirect
		response.sendRedirect(request.getContextPath() + Constants_Value.CART_URL);
	}
}
