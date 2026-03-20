package com.cinetick.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MovieController {

    private static final List<AdminMovie> movieDatabase = new ArrayList<>();
    private static final List<Map<String, String>> boughtTickets = new ArrayList<>();
    private static final Map<String, String> userDatabase = new HashMap<>();
    private boolean isUserLoggedIn = false;

    static {
        movieDatabase.add(new AdminMovie(1, "Spiderman: Brand New Day", "Active", "Superhero", "148 mins", 250.0));
        movieDatabase.add(new AdminMovie(2, "The Batman", "Active", "Action", "176 mins", 220.0));
        movieDatabase.add(new AdminMovie(3, "Avengers: Secret Wars", "Active", "Action", "192 mins", 300.0));
        movieDatabase.add(new AdminMovie(4, "Superman", "Active", "Action", "146 mins", 290.0));
        userDatabase.put("user", "123");
    }

    // Para gumana yung nav bar

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "index";
    }

    @GetMapping("/movies")
    public String movies(Model model) {
        model.addAttribute("movies", movieDatabase);
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "movies";
    }

    @GetMapping("/about")
    public String showAbout(Model model) {
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "about";
    }

    // Booking

    @GetMapping("/booking/{title}")
    public String showBookingPage(@PathVariable String title, Model model) {
        AdminMovie selected = movieDatabase.stream()
                .filter(m -> m.getTitle().equalsIgnoreCase(title))
                .findFirst().orElse(null);

        if (selected == null) return "redirect:/movies";

        // Dates
        List<Map<String, String>> dateList = new ArrayList<>();
        String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        for (int i = 0; i < 5; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, i);
            Map<String, String> date = new HashMap<>();
            date.put("formattedDate", cal.get(Calendar.DAY_OF_MONTH) + " " + months[cal.get(Calendar.MONTH)]);
            date.put("dayName", days[cal.get(Calendar.DAY_OF_WEEK) - 1]);
            dateList.add(date);
        }

        model.addAttribute("movie", selected);
        model.addAttribute("realDates", dateList);
        model.addAttribute("timeSlots", Arrays.asList("10:00 AM", "1:00 PM", "4:00 PM", "7:00 PM"));
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "booking";
    }

    @GetMapping("/seats/{title}")
    public String showSeatSelection(@PathVariable String title, Model model) {
        model.addAttribute("title", title);
        model.addAttribute("rows", Arrays.asList("A", "B", "C", "D", "E", "F"));
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "seats";
    }

    @PostMapping("/seats/proceed")
    public String handleProceed(@RequestParam String title, @RequestParam List<String> seats, Model model) {
        String seatStr = String.join(",", seats);

        if (!isUserLoggedIn) {
            model.addAttribute("title", title);
            model.addAttribute("seats", seatStr);
            return "user-login";
        }
        return "redirect:/checkout?title=" + title + "&seats=" + seatStr;
    }

    @GetMapping("/checkout")
    public String showCheckout(@RequestParam String title, @RequestParam(required=false) String seats, Model model) {
        if (!isUserLoggedIn) return "redirect:/user-login?title=" + title + "&seats=" + seats;
        if (seats == null || seats.isEmpty()) return "redirect:/seats/" + title;

        AdminMovie movie = movieDatabase.stream()
                .filter(m -> m.getTitle().equals(title))
                .findFirst().orElse(null);

        int seatCount = seats.split(",").length;
        double total = (movie != null) ? seatCount * movie.getPrice() : 0;

        model.addAttribute("title", title);
        model.addAttribute("selectedSeats", seats);
        model.addAttribute("seatCount", seatCount);
        model.addAttribute("totalPrice", total);
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "checkout";
    }

    @PostMapping("/process-payment")
    public String processPayment(@RequestParam double paymentAmount, @RequestParam double totalPrice,
                                 @RequestParam String title, @RequestParam String seats) {
        if (paymentAmount >= totalPrice) {
            Map<String, String> ticket = new HashMap<>();
            ticket.put("title", title);
            ticket.put("seats", seats);
            ticket.put("date", "Mar 16, 2026");
            ticket.put("hours", "1:00 PM");
            ticket.put("total", String.format("%.2f", totalPrice));
            ticket.put("paid", String.format("%.2f", paymentAmount));
            ticket.put("change", String.format("%.2f", paymentAmount - totalPrice));
            boughtTickets.add(ticket);
            return "redirect:/success?title=" + title;
        }
        return "redirect:/checkout?title=" + title + "&seats=" + seats + "&error=low_balance";
    }

    @GetMapping("/success")
    public String success(@RequestParam(required=false) String title, Model model) {
        model.addAttribute("title", (title != null) ? title : "Movie");
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "success";
    }

    @GetMapping("/mytickets")
    public String showMyTickets(Model model) {
        model.addAttribute("tickets", boughtTickets);
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "mytickets";
    }

    // login at registyer

    @GetMapping("/user-login")
    public String showLogin(@RequestParam(required=false) String title, @RequestParam(required=false) String seats, Model model) {
        model.addAttribute("title", title);
        model.addAttribute("seats", seats);
        model.addAttribute("isUserLoggedIn", isUserLoggedIn);
        return "user-login";
    }

    @PostMapping("/user-login")
    public String doLogin(@RequestParam String username, @RequestParam String password,
                          @RequestParam(required=false) String title, 
                          @RequestParam(required=false) String seats, Model model) {
       
        if (userDatabase.containsKey(username) && userDatabase.get(username).equals(password)) {
            isUserLoggedIn = true;
            return (title != null && !title.isEmpty()) ? 
                "redirect:/checkout?title=" + title + "&seats=" + seats : "redirect:/movies";
        }
        
        model.addAttribute("error", "Invalid Username or Password");
        return "user-login";
    }

    @PostMapping("/user-register")
    public String doRegister(@RequestParam String username, @RequestParam String password, Model model) {
        if (username.isEmpty() || password.isEmpty()) {
            model.addAttribute("error", "Fields cannot be empty");
            return "user-login"; 
        }

        if (userDatabase.containsKey(username)) {
            model.addAttribute("error", "Username already exists!");
            return "user-login";
        }
        userDatabase.put(username, password);
        isUserLoggedIn = true;
        return "redirect:/movies";
    }

    @GetMapping("/logout")
    public String logout() {
        isUserLoggedIn = false;
        return "redirect:/";
    }

    // admin

    @GetMapping("/admin-login")
    public String adminLogin() { return "admin-login"; }

    @PostMapping("/admin-login")
    public String doAdminLogin(@RequestParam String username, @RequestParam String password, Model model) {
        if ("admin".equals(username) && "123".equals(password)) return "redirect:/admin/movies";
        model.addAttribute("error", "Access Denied");
        return "admin-login";
    }

    @GetMapping("/admin/movies")
    public String adminMovies(Model model) {
        model.addAttribute("adminMovies", movieDatabase);
        return "admin-movie";
    }

    @GetMapping("/admin/movies/create")
    public String showCreateForm() {
    return "admin-movie-create";
}

    @GetMapping("/admin/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", boughtTickets);
        return "admin-orders";
    }
    @GetMapping("/admin/movies/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model) {
    AdminMovie target = movieDatabase.stream()
            .filter(m -> m.getId() == id)
            .findFirst()
            .orElse(null);


    if (target == null) return "redirect:/admin/movies";


    model.addAttribute("movie", target);
    return "admin-movie-edit";
}


    @PostMapping("/admin/movies/update")
    public String updateMovie(@RequestParam int id, @RequestParam String title, 
                          @RequestParam String genre, @RequestParam String runtime,
                          @RequestParam double price, @RequestParam String status) {
    
    for (AdminMovie m : movieDatabase) {
        if (m.getId() == id) {
            m.setTitle(title);
            m.setGenre(genre);
            m.setRuntime(runtime);
            m.setPrice(price);
            m.setStatus(status);
            break;
        }
    }
    return "redirect:/admin/movies";
}

    @PostMapping("/admin/movies/add")
    public String addMovie(@RequestParam String title, @RequestParam String genre, @RequestParam String runtime,
                           @RequestParam double price, @RequestParam String status) {
        int nextId = movieDatabase.isEmpty() ? 1 : movieDatabase.get(movieDatabase.size() - 1).getId() + 1;
        movieDatabase.add(new AdminMovie(nextId, title, status, genre, runtime, price));
        return "redirect:/admin/movies";
    }

    @GetMapping("/admin/movies/delete/{id}")
    public String deleteMovie(@PathVariable int id) {
        movieDatabase.removeIf(m -> m.getId() == id);
        return "redirect:/admin/movies";
    }

    // classes

    public static class AdminMovie {
        private final int id;
        private String title, status, genre, runtime;
        private double price;

        public AdminMovie(int id, String t, String s, String g, String r, double p) {
            this.id = id; this.title = t; this.status = s; this.genre = g; this.runtime = r; this.price = p;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public String getRuntime() { return runtime; }
        public void setRuntime(String runtime) { this.runtime = runtime; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}