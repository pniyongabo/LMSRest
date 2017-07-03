package com.gcit.lms;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gcit.lms.dao.AuthorDAO;
import com.gcit.lms.dao.BookCopyDAO;
import com.gcit.lms.dao.BookDAO;
import com.gcit.lms.dao.BookLoanDAO;
import com.gcit.lms.dao.BorrowerDAO;
import com.gcit.lms.dao.BranchDAO;
import com.gcit.lms.dao.GenreDAO;
import com.gcit.lms.dao.PublisherDAO;
import com.gcit.lms.entity.Author;
import com.gcit.lms.entity.Book;
import com.gcit.lms.entity.BookCopy;
import com.gcit.lms.entity.BookLoan;
import com.gcit.lms.entity.Borrower;
import com.gcit.lms.entity.Branch;
import com.gcit.lms.entity.Publisher;
import com.gcit.lms.service.AdminService;
import com.gcit.lms.service.BorrowerService;
import com.gcit.lms.service.LibrarianService;

@RestController
public class HomeController {
	
	@Autowired
	AdminService adminService;
	
	@Autowired
	LibrarianService librarianService;
	
	@Autowired
	BorrowerService borrowerService;
	
	@Autowired
	AuthorDAO adao;

	@Autowired
	BookDAO bdao;

	@Autowired
	GenreDAO gdao;

	@Autowired
	PublisherDAO pdao;

	@Autowired
	BranchDAO brdao;

	@Autowired
	BorrowerDAO bodao;

	@Autowired
	BookLoanDAO bldao;
	
	@Autowired
	BookCopyDAO bcdao;
	
	//================================================================================
    // Welcome page 
    //================================================================================
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		return "Welcome to the GCIT library management app.";
	}
	
	//================================================================================
    // LIBRARIAN SERVICES
    //================================================================================	
	
	@RequestMapping(value = "/librarian", method = RequestMethod.GET, produces="application/json")
	public List<Branch> librarian() throws SQLException { 
		List<Branch> branches =  brdao.readAllBranches();
		for (Branch br: branches){
			br.setBooks(bdao.readAllBooksByBranchId(br.getBranchId()));
			br.setBookLoans(bldao.readAllBookLoansByBranchId(br.getBranchId()));
		}
		return branches;
	}
	
	@RequestMapping(value = "editBranchLib", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<Branch> editBranchLib(@RequestBody Branch branch) throws SQLException {
		brdao.updateBranch(branch);
		return librarian();
	}
	
	@RequestMapping(value = "/l_viewbookcopies", method = RequestMethod.GET, 
			produces="application/json")
	public List<BookCopy> l_viewbookcopies() throws SQLException {
		return bcdao.readAllBookCopies();
	}
	
	@RequestMapping(value = "/l_viewbookcopiesbybranch", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<BookCopy> l_viewbookcopiesbybranch(@RequestBody Branch branch) throws SQLException {
		return bcdao.readAllBookCopiesByBranch(branch);
	}
	
	@RequestMapping(value = "/l_viewbookcopiesbybook", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<BookCopy> l_viewbookcopiesbybook(@RequestBody Book book) throws SQLException {
		return bcdao.readAllBookCopiesByBook(book);
	}
	
	@RequestMapping(value = "editBookCopy", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<BookCopy> editBookCopy(@RequestBody BookCopy bookcopy) throws SQLException {
		bcdao.updateBookCopy(bookcopy);
		return l_viewbookcopies();
	}
	
	@RequestMapping(value = "deleteBookCopy", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<BookCopy> deleteBookCopy(@RequestBody BookCopy bookcopy) throws SQLException {
		bcdao.deleteBookCopy(bookcopy);
		return l_viewbookcopies();
	}
	
	//================================================================================
    // BORROWER SERVICES
    //================================================================================
	
	@RequestMapping(value = "/b_viewbookloansbyuser", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<BookLoan> b_viewbookloansbyuser(@RequestBody Borrower borrower) throws SQLException {
		return bldao.readAllDueBookLoansByBorrower(borrower);
	}
	
	@RequestMapping(value = "borrowerLogin", method = RequestMethod.POST, consumes="application/json")
	public String borrowerLogin(@RequestBody Borrower borrower) throws SQLException {
		if ((borrower.getCardNo() != null) && (borrower.getCardNo() > 0)){
			Integer num =  bodao.getBorrowersCountByPk(borrower.getCardNo());
			if(num > 0){
				return "Logged in successfully!";
			}else{
				return "Please enter a valid card number!";
			}
		}
		return "Borrower card number should be a positive number!";
	}
	
	@RequestMapping(value = "/b_viewbooksavailableatbranch", method = RequestMethod.POST, 
			consumes="application/json",produces="application/json")
	public List<BookCopy> b_viewbooksavailableatbranch(@RequestBody Branch branch) throws SQLException { 
		return librarianService.getAllBookCopiesOwnedBy(branch);
	}
	
	@RequestMapping(value = "/checkOutBook", method = RequestMethod.GET)
	public String checkOutBook(Model model,@RequestParam("cardNo") Integer cardNo,
			@RequestParam("branchId") Integer branchId, @RequestParam("bookId") Integer bookId) throws SQLException {
		Borrower borrower = borrowerService.getBorrowerByPK(cardNo);
		borrowerService.checkOutBook(bookId, branchId, cardNo);
		String message = borrower.getName()+". Book borrowed successfully";
		model.addAttribute("message", message);
		model.addAttribute("cardNo", cardNo);
		return "b_pickaction";
	}
	
	// return book copy
	
			// check out book copy
	
	@RequestMapping(value = "/returnBook", method = RequestMethod.GET)
	public String returnBook(Model model,@RequestParam("cardNo") Integer cardNo,
			@RequestParam("branchId") Integer branchId, 
			@RequestParam("bookId") Integer bookId,
			@RequestParam("dateOut") String dateOut) throws SQLException {
		
		BookLoan bookLoan = new BookLoan();
		dateOut = dateOut.replaceAll("T", " ");
		
		bookLoan.setBook(adminService.getBookByPK(bookId));
		bookLoan.setBorrower(adminService.getBorrowerByPK(cardNo));
		bookLoan.setBranch(adminService.getBranchByPK(branchId));
		bookLoan.setDateOut(dateOut);
		borrowerService.returnBook(bookLoan);
		
		Borrower borrower = borrowerService.getBorrowerByPK(cardNo);
		String message = "Thank you "+borrower.getName()+"! Book returned successfully!";
		model.addAttribute("message", message);
		return null; //b_viewbookloans(model,cardNo);
	}
	
	//================================================================================
    // BELOW HERE IS ADMIN TERRITORY
    //================================================================================

	//================================================================================
    // Books pages
    //================================================================================
	
//	@RequestMapping(value = "/a_book", method = RequestMethod.GET)
//	public String a_book() {
//		return "a_book";
//	}
//	
	
	@RequestMapping(value = "addBook", method = RequestMethod.POST, consumes="application/json")
	public String addBook(@RequestBody Book book) throws SQLException {
		bdao.addBook(book);
		return "Book Added - Success is in the AIR!";
	}
	
	@RequestMapping(value = "editBook", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<Book> editBook(@RequestBody Book book) throws SQLException {
		bdao.updateBook(book);
		return a_viewBooks();
	}
	
	@RequestMapping(value = "/a_viewbooks", method = RequestMethod.GET, produces="application/json")
	public List<Book> a_viewBooks() throws SQLException { 
		List<Book> books =  bdao.readAllBooks();
		for (Book b: books){
			b.setAuthors(adao.readAllAuthorsByBookId(b.getBookId()));
			b.setGenres(gdao.readAllGenresByBookId(b.getBookId()));
			b.setPublisher(pdao.readPublisherByBookId(b.getBookId()));
		}
		return books;
	}
	
	@RequestMapping(value = "/a_viewbooks/{pageNo}", method = RequestMethod.GET, produces="application/json")
	public List<Book> a_viewBooks(@PathVariable Integer pageNo) throws SQLException { 
		List<Book> books =  bdao.readAllBooks(pageNo);
		for (Book b: books){
			b.setAuthors(adao.readAllAuthorsByBookId(b.getBookId()));
			b.setGenres(gdao.readAllGenresByBookId(b.getBookId()));
			b.setPublisher(pdao.readPublisherByBookId(b.getBookId()));
		}
		return books;
	}
	
	@RequestMapping(value = "/a_viewbooks/{pageNo}/{searchString}", method = RequestMethod.GET, produces="application/json")
	public List<Book> a_viewBooks(@PathVariable Integer pageNo, 
			@PathVariable String searchString) throws SQLException { 
		List<Book> books =  bdao.readAllBooksByName(pageNo, searchString);
		for (Book b: books){
			b.setAuthors(adao.readAllAuthorsByBookId(b.getBookId()));
			b.setGenres(gdao.readAllGenresByBookId(b.getBookId()));
			b.setPublisher(pdao.readPublisherByBookId(b.getBookId()));
		}
		return books;
	}
	
	@RequestMapping(value = "deleteBook", method = RequestMethod.POST, consumes="application/json ")
	public String deleteBook(@RequestBody Book book) throws SQLException {
		bdao.deleteBook(book);
		return "Book deleted successfully!";
	}
			
	
	//================================================================================
    // Authors pages
    //================================================================================

	
//	@RequestMapping(value = "/a_author", method = RequestMethod.GET)
//	public String a_author() {
//		return "a_author";
//	}
	
	@RequestMapping(value = "addAuthor", method = RequestMethod.POST, consumes="application/json")
	public String addAuthor(@RequestBody Author author) throws SQLException {
		adao.addAuthor(author);
		return "Author Added - Success is in the AIR!";
	}
	
	@RequestMapping(value = "editAuthor", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<Author> editBook(@RequestBody Author author) throws SQLException {
		adao.updateAuthor(author);
		return a_viewAuthors();
	}
	
	@RequestMapping(value = "/a_viewauthors/{pageNo}/{searchString}", method = RequestMethod.GET, produces="application/json")
	public List<Author> a_viewAuthors(@PathVariable Integer pageNo, 
			@PathVariable String searchString) throws SQLException { 
		List<Author> authors =  adao.readAllAuthorsByName(pageNo, searchString);
		for (Author a: authors){
			a.setBooks(bdao.readAllBooksByAuthorId(a.getAuthorId()));
		}
		return authors;
	}
	
	@RequestMapping(value = "/a_viewauthors/{pageNo}", method = RequestMethod.GET, produces="application/json")
	public List<Author> a_viewAuthors(@PathVariable Integer pageNo) throws SQLException { 
		List<Author> authors =  adao.readAllAuthors(pageNo);
		for (Author a: authors){
			a.setBooks(bdao.readAllBooksByAuthorId(a.getAuthorId()));
		}
		return authors;
	}
	
	@RequestMapping(value = "/a_viewauthors", method = RequestMethod.GET, produces="application/json")
	public List<Author> a_viewAuthors() throws SQLException { 
		List<Author> authors =  adao.readAllAuthors();
		for (Author a: authors){
			a.setBooks(bdao.readAllBooksByAuthorId(a.getAuthorId()));
		}
		return authors;
	}
	
	@RequestMapping(value = "deleteAuthor", method = RequestMethod.POST, consumes="application/json ")
	public String deleteAuthor(@RequestBody Author author) throws SQLException {
		adao.deleteAuthor(author);
		return "Author deleted successfully!";
	}
	
	//================================================================================
    // Borrowers pages
    //================================================================================
	
//	@RequestMapping(value = "/a_borrower", method = RequestMethod.GET)
//	public String a_borrower() {
//		return "a_borrower";
//	}

	@RequestMapping(value = "addBorrower", method = RequestMethod.POST, consumes="application/json")
	public String addBorrower(@RequestBody Borrower borrower) throws SQLException {
		bodao.addBorrower(borrower);
		return "borrower Added - Success is in the AIR!";
	}
	
	@RequestMapping(value = "editBorrower", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<Borrower> editBook(@RequestBody Borrower borrower) throws SQLException {
		bodao.updateBorrower(borrower);
		return a_viewborrowers();
	}
	
	@RequestMapping(value = "/a_viewborrowers/{pageNo}/{searchString}", method = RequestMethod.GET, produces="application/json")
	public List<Borrower> a_viewborrowers(@PathVariable Integer pageNo, 
			@PathVariable String searchString) throws SQLException { 
		List<Borrower> borrowers =  bodao.readAllBorrowersByName(pageNo, searchString);
		for (Borrower bo: borrowers){
			bo.setBookLoans(bldao.readAllBookLoansByCardNo(bo.getCardNo()));
		}
		return borrowers;
	}
	
	@RequestMapping(value = "/a_viewborrowers/{pageNo}", method = RequestMethod.GET, produces="application/json")
	public List<Borrower> a_viewborrowers(@PathVariable Integer pageNo) throws SQLException { 
		List<Borrower> borrowers =  bodao.readAllBorrowers(pageNo);
		for (Borrower bo: borrowers){
			bo.setBookLoans(bldao.readAllBookLoansByCardNo(bo.getCardNo()));
		}
		return borrowers;
	}
	
	@RequestMapping(value = "/a_viewborrowers", method = RequestMethod.GET, produces="application/json")
	public List<Borrower> a_viewborrowers() throws SQLException { 
		List<Borrower> borrowers =  bodao.readAllBorrowers();
		for (Borrower bo: borrowers){
			bo.setBookLoans(bldao.readAllBookLoansByCardNo(bo.getCardNo()));
		}
		return borrowers;
	}
	
	@RequestMapping(value = "deleteBorrower", method = RequestMethod.POST, consumes="application/json ")
	public String deleteBorrower(@RequestBody Borrower borrower) throws SQLException {
		bodao.deleteBorrower(borrower);
		return "borrower deleted successfully!";
	}
	
	//================================================================================
    // Branches pages
    //================================================================================
	
//	@RequestMapping(value = "/a_branch", method = RequestMethod.GET)
//	public String a_branch() {
//		return "a_branch";
//	}
	
	@RequestMapping(value = "addBranch", method = RequestMethod.POST, consumes="application/json")
	public String addBranch(@RequestBody Branch branch) throws SQLException {
		brdao.addBranch(branch);
		return "branch Added - Success is in the AIR!";
	}
	
	@RequestMapping(value = "editBranch", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<Branch> editBranch(@RequestBody Branch branch) throws SQLException {
		brdao.updateBranch(branch);
		return a_viewbranches();
	}
	
	@RequestMapping(value = "/a_viewbranches/{pageNo}/{searchString}", method = RequestMethod.GET, produces="application/json")
	public List<Branch> a_viewbranches(@PathVariable Integer pageNo, 
			@PathVariable String searchString) throws SQLException { 
		List<Branch> branches =  brdao.readAllBranchesByName(pageNo, searchString);
		for (Branch br: branches){
			br.setBooks(bdao.readAllBooksByBranchId(br.getBranchId()));
			br.setBookLoans(bldao.readAllBookLoansByBranchId(br.getBranchId()));
		}
		return branches;
	}
	
	@RequestMapping(value = "/a_viewbranches/{pageNo}", method = RequestMethod.GET, produces="application/json")
	public List<Branch> a_viewbranches(@PathVariable Integer pageNo) throws SQLException { 
		List<Branch> branches =  brdao.readAllBranches(pageNo);
		for (Branch br: branches){
			br.setBooks(bdao.readAllBooksByBranchId(br.getBranchId()));
			br.setBookLoans(bldao.readAllBookLoansByBranchId(br.getBranchId()));
		}
		return branches;
	}
	
	@RequestMapping(value = "/a_viewbranches", method = RequestMethod.GET, produces="application/json")
	public List<Branch> a_viewbranches() throws SQLException { 
		List<Branch> branches =  brdao.readAllBranches();
		for (Branch br: branches){
			br.setBooks(bdao.readAllBooksByBranchId(br.getBranchId()));
			br.setBookLoans(bldao.readAllBookLoansByBranchId(br.getBranchId()));
		}
		return branches;
	}
	
	@RequestMapping(value = "deleteBranch", method = RequestMethod.POST, consumes="application/json ")
	public String deleteBranch(@RequestBody Branch branch) throws SQLException {
		brdao.deleteBranch(branch);
		return "branch deleted successfully!";
	}
	
	//================================================================================
    // Publishers pages
    //================================================================================
	
//	@RequestMapping(value = "/a_publisher", method = RequestMethod.GET)
//	public String a_publisher() {
//		return "a_publisher";
//	}
	
	@RequestMapping(value = "addPublisher", method = RequestMethod.POST, consumes="application/json")
	public String addPublisher(@RequestBody Publisher publisher) throws SQLException {
		pdao.addPublisher(publisher);
		return "publisher Added - Success is in the AIR!";
	}
	
	@RequestMapping(value = "editPublisher", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<Publisher> editPulisher(@RequestBody Publisher publisher) throws SQLException {
		pdao.updatePublisher(publisher);
		return a_viewpublishers();
	}
	
	@RequestMapping(value = "/a_viewpublishers/{pageNo}/{searchString}", method = RequestMethod.GET, produces="application/json")
	public List<Publisher> a_viewpublishers(@PathVariable Integer pageNo, 
			@PathVariable String searchString) throws SQLException { 
		List<Publisher> publishers =  pdao.readAllPublishersByName(pageNo, searchString);
		for (Publisher p:publishers){
			p.setBooks(bdao.readAllBooksByPublisherId(p.getPublisherId()));
		}
		return publishers;
	}
	
	@RequestMapping(value = "/a_viewpublishers/{pageNo}", method = RequestMethod.GET, produces="application/json")
	public List<Publisher> a_viewpublishers(@PathVariable Integer pageNo) throws SQLException { 
		List<Publisher> publishers =  pdao.readAllPublishers(pageNo);
		for (Publisher p:publishers){
			p.setBooks(bdao.readAllBooksByPublisherId(p.getPublisherId()));
		}
		return publishers;
	}
	
	@RequestMapping(value = "/a_viewpublishers", method = RequestMethod.GET, produces="application/json")
	public List<Publisher> a_viewpublishers() throws SQLException { 
		List<Publisher> publishers =  pdao.readAllPublishers();
		for (Publisher p:publishers){
			p.setBooks(bdao.readAllBooksByPublisherId(p.getPublisherId()));
		}
		return publishers;
	}
	
	@RequestMapping(value = "deletePublisher", method = RequestMethod.POST, consumes="application/json ")
	public String deletePublisher(@RequestBody Publisher publisher) throws SQLException {
		pdao.deletePublisher(publisher);
		return "publisher deleted successfully!";
	}
	
	//================================================================================
    // Loans pages
    //================================================================================
	
	
	
	@RequestMapping(value = "editBookLoan", method = RequestMethod.POST, 
			consumes="application/json", produces="application/json")
	public List<BookLoan> editBookLoan(@RequestBody BookLoan bookloan) throws SQLException {
		bldao.updateBookLoan(bookloan);
		return a_viewbookloans();
	}
	
	@RequestMapping(value = "/a_viewbookloans", method = RequestMethod.GET, produces="application/json")
	public List<BookLoan> a_viewbookloans() throws SQLException {
		return bldao.readAllBookLoans();
	}
	
	@RequestMapping(value = "/a_viewbookloans/{pageNo}", method = RequestMethod.GET, produces="application/json")
	public List<BookLoan> a_viewbookloans(@PathVariable Integer pageNo) throws SQLException {
		return bldao.readAllBookLoans(pageNo);
	}
	
	@RequestMapping(value = "/a_viewbookloans/{pageNo}/{searchString}", method = RequestMethod.GET, produces="application/json")
	public List<BookLoan> a_viewbookloans(@PathVariable Integer pageNo, 
			@PathVariable String searchString) throws SQLException {
		return bldao.readAllBookLoansByDateOut(pageNo, searchString);
	}
}
