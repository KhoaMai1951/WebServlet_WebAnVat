package servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.CategoryDAO;
import dao.ProductDAO;
import entities.Cart;
import ultilities.Constants_Value;

/**
 * Servlet implementation class View_CheckoutController
 */
@WebServlet("/View_CheckoutController")
public class View_CheckoutController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Cart cart = new Cart();
    /**
     * @see HttpServlet#HttpServlet()
     */
    public View_CheckoutController() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			// get all products
			request.setAttribute("productList", ProductDAO.getAll());
			// get all categories
			request.setAttribute("categoryList", CategoryDAO.getAll());
			// get session
			if(request.getSession().getAttribute(Constants_Value.SESSION_CART) != null)
			{
				cart = (Cart) request.getSession().getAttribute(Constants_Value.SESSION_CART);
				request.setAttribute("items", cart.getItems()); //get items in cart
				request.setAttribute("sumPrice", cart.getSumPrice()); //get sum price
			}
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		request.getRequestDispatcher("/pages/checkout.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}