package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.model.Models.Choice;
import com.mycompany.petadoption.service.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public final class PetsPanel extends JPanel {
  private final PetService pets;
  private final AdoptionService adoptions;
  private final Session session;
  private final boolean admin;
  private final String currency;
  private final JTextField search = Theme.field("Search by name or breed");
  private final JComboBox<Choice> species = new JComboBox<>();
  private final JComboBox<Breed> breeds = new JComboBox<>();
  private final JComboBox<String> status =
      new JComboBox<>(new String[] {"All statuses", "AVAILABLE", "ON_HOLD", "ADOPTED", "ARCHIVED"});
  private final JPanel content = Theme.panel(new BorderLayout());
  private final JLabel count = Theme.muted("Loading companions…");
  private List<Breed> breedList = List.of();
  private List<Choice> speciesList = List.of();
  private long request;

  public PetsPanel(
      PetService pets, AdoptionService adoptions, Session session, boolean admin, String currency) {
    this.pets = pets;
    this.adoptions = adoptions;
    this.session = session;
    this.admin = admin;
    this.currency = currency;
    setLayout(new BorderLayout(0, 18));
    setOpaque(false);
    JButton find = Theme.primary("Search", this::reload);
    search.addActionListener(e -> reload());
    species.setPreferredSize(new Dimension(140, 38));
    breeds.setPreferredSize(new Dimension(170, 38));
    species.getAccessibleContext().setAccessibleName("Species filter");
    breeds.getAccessibleContext().setAccessibleName("Breed filter");
    JPanel filters = Theme.flow(search, species, breeds, find);
    if (admin) filters.add(status);
    JPanel top = Theme.column(filters, count);
    if (admin) {
      JPanel actions =
          Theme.flow(
              Theme.primary("+ Add pet", () -> editor(null)),
              Theme.button("Manage species & breeds", this::catalog));
      top = Theme.column(actions, filters, count);
    }
    add(top, BorderLayout.NORTH);
    add(content);
    species.addActionListener(e -> updateBreeds());
    SwingUtilities.invokeLater(this::catalogLoad);
  }

  private void catalogLoad() {
    Ui.work(
        this,
        null,
        () -> new Object[] {pets.species(session), pets.breeds(session)},
        data -> {
          @SuppressWarnings("unchecked")
          List<Choice> s = (List<Choice>) data[0];
          @SuppressWarnings("unchecked")
          List<Breed> b = (List<Breed>) data[1];
          speciesList = s;
          breedList = b;
          species.removeAllItems();
          species.addItem(new Choice(0, "All species"));
          s.forEach(species::addItem);
          updateBreeds();
          reload();
        });
  }

  private void updateBreeds() {
    Choice s = (Choice) species.getSelectedItem();
    breeds.removeAllItems();
    breeds.addItem(new Breed(0, 0, "", "All breeds"));
    breedList.stream()
        .filter(b -> s == null || s.id() == 0 || b.speciesId() == s.id())
        .forEach(breeds::addItem);
  }

  private void reload() {
    long version = ++request;
    Choice sp = (Choice) species.getSelectedItem();
    Breed br = (Breed) breeds.getSelectedItem();
    String term = search.getText();
    String filter = status.getSelectedIndex() == 0 ? null : (String) status.getSelectedItem();
    count.setText("Loading companions…");
    Ui.work(
        this,
        null,
        () ->
            pets.search(
                session,
                term,
                sp == null || sp.id() == 0 ? null : sp.id(),
                br == null || br.id() == 0 ? null : br.id(),
                filter),
        list -> {
          if (version != request) return;
          content.removeAll();
          count.setText(list.size() + " companions found");
          if (list.isEmpty())
            content.add(
                Theme.muted(
                    admin
                        ? "No pets match. Add your first pet or change the filters."
                        : "No companions match these filters. Try another search."),
                BorderLayout.NORTH);
          else if (admin) managementTable(list);
          else cards(list);
          content.revalidate();
          content.repaint();
        });
  }

  private void cards(List<Pet> list) {
    JPanel grid = Theme.panel(new GridLayout(0, 3, 18, 18));
    for (Pet p : list) {
      JPanel card = Theme.card();
      card.setBorder(BorderFactory.createLineBorder(Theme.LINE));
      card.add(new PhotoPanel(p.imagePath(), 230, 165), BorderLayout.NORTH);
      JPanel details =
          Theme.column(
              Theme.title(p.name(), 23),
              Theme.muted(p.species() + " · " + p.breed()),
              Theme.muted(p.age() + " · " + p.sex()),
              Theme.muted("AVAILABLE FOR ADOPTION"),
              Theme.primary("Meet " + p.name(), () -> details(p)));
      details.setBorder(BorderFactory.createEmptyBorder(8, 16, 10, 16));
      card.add(details);
      grid.add(card);
    }
    JPanel wrapper = Theme.panel(new BorderLayout());
    wrapper.add(grid, BorderLayout.NORTH);
    JScrollPane scroll = Theme.scroll(wrapper);
    content.add(scroll);
    scroll.addComponentListener(
        new java.awt.event.ComponentAdapter() {
          @Override
          public void componentResized(java.awt.event.ComponentEvent e) {
            int cols = Math.max(1, Math.min(3, scroll.getWidth() / 255));
            ((GridLayout) grid.getLayout()).setColumns(cols);
            grid.revalidate();
          }
        });
  }

  private void managementTable(List<Pet> list) {
    List<List<Object>> rows = new ArrayList<>();
    for (Pet p : list)
      rows.add(List.of(p.name(), p.species(), p.breed(), p.age(), p.sex(), p.status(), p.fee()));
    JTable table =
        Ui.table(
            new Report(
                List.of(
                    "Name", "Species", "Breed", "Age", "Sex", "Status", "Fee (" + currency + ")"),
                rows));
    content.add(Theme.scroll(table));
    java.util.function.Supplier<Pet> selected =
        () -> {
          if (table.getSelectedRow() < 0) throw new IllegalArgumentException("Select a pet first.");
          return list.get(table.convertRowIndexToModel(table.getSelectedRow()));
        };
    content.add(
        Theme.flow(
            Theme.button("View details", () -> Ui.action(this, () -> details(selected.get()))),
            Theme.button("Edit pet", () -> Ui.action(this, () -> editor(selected.get()))),
            Theme.button(
                "Archive pet",
                () ->
                    Ui.action(
                        this,
                        () -> {
                          Pet p = selected.get();
                          if (Ui.confirm(
                              this,
                              "Archive " + p.name() + "? Pending applications will be closed."))
                            Ui.work(
                                this,
                                null,
                                () -> {
                                  pets.archive(session, p);
                                  return true;
                                },
                                v -> reload());
                        }))),
        BorderLayout.SOUTH);
  }

  private void details(Pet p) {
    JTextArea description = Theme.area(5);
    description.setText(p.description());
    description.setEditable(false);
    JPanel information =
        Theme.column(
            Theme.title(p.name(), 32),
            Theme.muted(p.species() + " · " + p.breed() + " · " + p.age() + " · " + p.sex()),
            Theme.muted(p.status() + " · Adoption fee " + currency + " " + p.fee()),
            new PhotoPanel(p.imagePath(), 490, 240),
            description);
    JPanel details = Theme.panel(new BorderLayout(0, 16));
    details.add(Theme.scroll(information));
    JButton apply = Theme.primary("Apply to adopt " + p.name(), () -> {});
    if (!admin) details.add(apply, BorderLayout.SOUTH);
    JDialog dialog = Ui.dialog(this, "Meet " + p.name(), details);
    if (!admin) {
      apply.addActionListener(
          event -> {
            dialog.dispose();
            application(p);
          });
    }
    dialog.setVisible(true);
  }

  private void application(Pet p) {
    JTextArea motivation = Theme.area(5), housing = Theme.area(3);
    JCheckBox consent = new JCheckBox("I can provide ongoing care and agree to a shelter review.");
    consent.setOpaque(false);
    JButton submit = Theme.primary("Submit application", () -> {});
    JPanel form =
        Theme.column(
            Theme.title("A home for " + p.name(), 26),
            Theme.muted("One pending application at a time. Approval confirms the adoption."),
            Theme.fieldRow(
                "Why would you like to adopt? Tell us about your experience.",
                Theme.scroll(motivation)),
            Theme.fieldRow("Your housing, household and other animals", Theme.scroll(housing)),
            consent,
            submit);
    JDialog d = Ui.dialog(this, "Adoption application", Theme.scroll(form));
    submit.addActionListener(
        e -> {
          String m = motivation.getText(), h = housing.getText();
          boolean agreed = consent.isSelected();
          Ui.work(
              d,
              submit,
              () -> adoptions.apply(session, p.id(), m, h, agreed),
              v -> {
                d.dispose();
                Ui.message(this, "Application submitted. Follow its status in My applications.");
                reload();
              });
        });
    d.setVisible(true);
  }

  private void editor(Pet p) {
    if (breedList.isEmpty()) {
      Ui.message(this, "Add a species and breed before adding a pet.");
      return;
    }
    PetEditor.open(this, p, breedList, pets, session, currency, this::reload);
  }

  private void catalog() {
    JTextField speciesName = Theme.field("Species name"), breedName = Theme.field("Breed name");
    JComboBox<Choice> choice = new JComboBox<>(speciesList.toArray(Choice[]::new));
    JButton addSpecies = Theme.primary("Add species", () -> {}),
        addBreed = Theme.primary("Add breed", () -> {});
    JDialog d =
        Ui.dialog(
            this,
            "Species & breeds",
            Theme.column(
                Theme.fieldRow("New species", speciesName),
                addSpecies,
                Theme.fieldRow("Species for new breed", choice),
                Theme.fieldRow("New breed", breedName),
                addBreed));
    addSpecies.addActionListener(
        e -> {
          String n = speciesName.getText();
          Ui.work(
              d,
              addSpecies,
              () -> {
                pets.addSpecies(session, n);
                return true;
              },
              v -> {
                d.dispose();
                catalogLoad();
              });
        });
    addBreed.addActionListener(
        e ->
            Ui.action(
                d,
                () -> {
                  Choice c = (Choice) choice.getSelectedItem();
                  if (c == null) throw new IllegalArgumentException("Add a species first.");
                  String n = breedName.getText();
                  Ui.work(
                      d,
                      addBreed,
                      () -> {
                        pets.addBreed(session, c.id(), n);
                        return true;
                      },
                      v -> {
                        d.dispose();
                        catalogLoad();
                      });
                }));
    d.setVisible(true);
  }
}
