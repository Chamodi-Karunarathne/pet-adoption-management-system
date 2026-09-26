package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.config.*;
import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.service.*;
import java.awt.*;
import javax.swing.*;

public final class MainFrame extends JFrame {
  private final AuthService auth;
  private final PetService pets;
  private final AdoptionService adoptions;
  private final AdminService admin;
  private final AppConfig config;
  private Session session;
  private User user;
  private JPanel body;
  private JLabel heading;
  private long navigation;

  public MainFrame(Database db, AppConfig config) {
    super("woof. — Pet Adoption");
    this.config = config;
    auth = new AuthService(db);
    pets = new PetService(db, auth);
    adoptions = new AdoptionService(db, auth);
    admin = new AdminService(db, auth);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(980, 720));
    setSize(1280, 840);
    setLocationRelativeTo(null);
    signIn();
  }

  private void signIn() {
    navigation++;
    auth.logout(session);
    session = null;
    user = null;
    setContentPane(new AuthPanel(auth, this::signedIn));
    revalidate();
    repaint();
  }

  private void signedIn(Session s) {
    session = s;
    Ui.work(
        this,
        null,
        () -> auth.current(s),
        u -> {
          user = u;
          shell();
          home();
        });
  }

  private void shell() {
    JPanel root = Theme.panel(new BorderLayout());
    JPanel side = new JPanel(new BorderLayout(0, 24));
    side.setBackground(Theme.GREEN);
    side.setPreferredSize(new Dimension(235, 0));
    side.setBorder(BorderFactory.createEmptyBorder(30, 20, 24, 20));
    JLabel brand = Theme.title("woof.", 38);
    brand.setForeground(Color.WHITE);
    side.add(
        Theme.column(
            brand,
            white(user.role() == Role.ADMIN ? "SHELTER WORKSPACE" : "YOUR ADOPTION JOURNEY")),
        BorderLayout.NORTH);
    JPanel links = Theme.panel(new GridLayout(0, 1, 0, 9));
    nav(links, "home", this::home);
    if (user.role() == Role.ADMIN) {
      nav(
          links,
          "pets",
          () ->
              show(
                  "Pet management",
                  new PetsPanel(pets, adoptions, session, true, config.currency())));
      nav(
          links,
          "applications",
          () -> show("Adoption requests", new RequestsPanel(adoptions, admin, session, true)));
      nav(
          links,
          "inventory",
          () -> show("Inventory & resources", new InventoryPanel(admin, session)));
      nav(links, "users", () -> show("People & access", new UsersPanel(admin, session)));
      nav(links, "reports", () -> show("Reports", new ReportsPanel(admin, session)));
    } else {
      nav(links, "browse", this::browse);
      nav(
          links,
          "requests",
          () -> show("My applications", new RequestsPanel(adoptions, admin, session, false)));
    }
    nav(
        links,
        "profile",
        () ->
            show(
                "My profile",
                new ProfilePanel(
                    auth,
                    session,
                    user,
                    () -> Ui.work(this, null, () -> auth.current(session), u -> user = u))));
    JPanel navContainer = Theme.panel(new BorderLayout());
    navContainer.add(links, BorderLayout.NORTH);
    side.add(navContainer);
    JButton logout = Theme.button(Strings.get("logout"), this::signIn);
    side.add(Theme.column(white(user.name()), logout), BorderLayout.SOUTH);
    root.add(side, BorderLayout.WEST);
    JPanel workspace = Theme.panel(new BorderLayout(0, 24));
    workspace.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
    heading = Theme.title("Home", 25);
    JPanel top = Theme.panel(new BorderLayout());
    top.add(heading);
    top.add(
        Theme.muted(user.role() == Role.ADMIN ? "ADMINISTRATOR" : "ADOPTER"), BorderLayout.EAST);
    workspace.add(top, BorderLayout.NORTH);
    body = Theme.panel(new BorderLayout());
    workspace.add(body);
    root.add(workspace);
    setContentPane(root);
    revalidate();
    repaint();
  }

  private JLabel white(String text) {
    JLabel l = new JLabel(text);
    l.setForeground(new Color(217, 232, 218));
    return l;
  }

  private void nav(JPanel p, String key, Runnable action) {
    JButton b = Theme.button(Strings.get(key), action);
    b.setHorizontalAlignment(SwingConstants.LEFT);
    b.setBackground(Theme.GREEN);
    b.setForeground(Color.WHITE);
    b.setBorderPainted(false);
    p.add(b);
  }

  private void show(String title, JComponent component) {
    navigation++;
    heading.setText(title);
    body.removeAll();
    body.add(component);
    body.revalidate();
    body.repaint();
  }

  private void browse() {
    show("Find a companion", new PetsPanel(pets, adoptions, session, false, config.currency()));
  }

  private void home() {
    JPanel page = Theme.panel(new BorderLayout(0, 24));
    show(Strings.get("home"), page);
    long current = navigation;
    JPanel hero = Theme.card();
    hero.setBackground(new Color(231, 237, 220));
    hero.add(
        Theme.column(
            Theme.muted("WELCOME TO WOOF"),
            Theme.title(
                user.role() == Role.ADMIN
                    ? "Good care starts with you."
                    : "A little love. A whole new life.",
                30),
            Theme.muted(
                user.role() == Role.ADMIN
                    ? "Keep every companion and every application moving forward."
                    : "Meet your next companion and follow your adoption journey."),
            Theme.primary(
                user.role() == Role.ADMIN ? "Manage companions" : "Meet the pets",
                () -> {
                  if (user.role() == Role.ADMIN)
                    show(
                        "Pet management",
                        new PetsPanel(pets, adoptions, session, true, config.currency()));
                  else browse();
                })),
        BorderLayout.CENTER);
    hero.add(new PhotoPanel("/images/welcome.jpg", 270, 240), BorderLayout.EAST);
    page.add(hero, BorderLayout.NORTH);
    JPanel center = Theme.panel(new BorderLayout(0, 24));
    JLabel loading = Theme.muted("Loading your shelter overview…");
    center.add(loading, BorderLayout.NORTH);
    page.add(center);
    Ui.work(
        this,
        null,
        () -> admin.dashboard(session),
        stats -> {
          if (current != navigation) return;
          center.removeAll();
          JPanel cards = Theme.panel(new GridLayout(0, Math.min(3, stats.size()), 16, 16));
          stats.forEach(
              (label, value) -> {
                JPanel card = Theme.card();
                card.add(Theme.column(Theme.title(value.toString(), 32), Theme.muted(label)));
                cards.add(card);
              });
          center.add(cards, BorderLayout.NORTH);
          JTextArea guide = Theme.area(5);
          guide.setEditable(false);
          guide.setBackground(Theme.BACKGROUND);
          guide.setText(
              user.role() == Role.ADMIN
                  ? "Your daily checklist\n\n"
                      + "Review pending applications, check pet availability, and record stock"
                      + " changes with a reason. Reports reflect the latest saved records."
                  : "Your adoption journey\n\n"
                      + "1. Find a companion and read their profile.\n"
                      + "2. Tell us about your home and submit one application at a time.\n"
                      + "3. Follow the decision in My applications. The shelter team will"
                      + " coordinate the next steps.");
          center.add(guide);
          center.revalidate();
          center.repaint();
        });
  }
}
