package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.service.*;
import com.mycompany.petadoption.util.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public final class PetEditor {
  private PetEditor() {}

  public static void open(
      Component owner,
      Pet pet,
      List<Breed> breeds,
      PetService service,
      Session session,
      String currency,
      Runnable reload) {
    JTextField name = Theme.field("Pet name"),
        birth = Theme.field("YYYY-MM-DD"),
        fee = Theme.field("0.00");
    JComboBox<Breed> breed = new JComboBox<>(breeds.toArray(Breed[]::new));
    breed.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList<?> l, Object v, int i, boolean selected, boolean focused) {
            return super.getListCellRendererComponent(
                l, v instanceof Breed b ? b.species() + " / " + b.name() : v, i, selected, focused);
          }
        });
    JComboBox<String> sex = new JComboBox<>(new String[] {"Unknown", "Male", "Female"});
    JComboBox<PetStatus> status =
        new JComboBox<>(
            pet != null && pet.status() == PetStatus.ADOPTED
                ? new PetStatus[] {PetStatus.ADOPTED}
                : new PetStatus[] {PetStatus.AVAILABLE, PetStatus.ON_HOLD, PetStatus.ARCHIVED});
    JTextArea description = Theme.area(4);
    String[] image = {pet == null ? "" : pet.imagePath()};
    JLabel imageLabel = Theme.muted(image[0].isEmpty() ? "No photo selected" : "Photo attached");
    JButton choose = Theme.button("Choose photo…", () -> {}),
        save = Theme.primary("Save pet", () -> {});
    if (pet != null) {
      name.setText(pet.name());
      birth.setText(pet.birthDate().toString());
      fee.setText(pet.fee().toPlainString());
      sex.setSelectedItem(pet.sex());
      status.setSelectedItem(pet.status());
      description.setText(pet.description());
      breeds.stream()
          .filter(b -> b.id() == pet.breedId())
          .findFirst()
          .ifPresent(breed::setSelectedItem);
    } else fee.setText("0.00");
    JPanel form =
        Theme.column(
            Theme.fieldRow("Name", name),
            Theme.fieldRow("Species / breed", breed),
            Theme.fieldRow("Birth date (estimated if necessary)", birth),
            Theme.fieldRow("Sex", sex),
            Theme.fieldRow("Adoption fee (" + currency + ")", fee),
            Theme.fieldRow("Availability", status),
            Theme.fieldRow("About this pet / care needs", Theme.scroll(description)),
            Theme.flow(choose, imageLabel));
    JPanel editor = Theme.panel(new BorderLayout(0, 16));
    editor.add(Theme.scroll(form));
    editor.add(save, BorderLayout.SOUTH);
    JDialog d = Ui.dialog(owner, pet == null ? "Add a companion" : "Edit " + pet.name(), editor);
    choose.addActionListener(
        e -> {
          JFileChooser picker = new JFileChooser();
          picker.setFileFilter(
              new javax.swing.filechooser.FileNameExtensionFilter(
                  "Pet photos (PNG, JPEG)", "png", "jpg", "jpeg"));
          if (picker.showOpenDialog(d) == JFileChooser.APPROVE_OPTION) {
            var file = picker.getSelectedFile().toPath();
            Ui.work(
                d,
                choose,
                () -> Images.importFile(file),
                value -> {
                  image[0] = value;
                  imageLabel.setText("Photo attached");
                });
          }
        });
    save.addActionListener(
        e ->
            Ui.action(
                d,
                () -> {
                  Breed b = (Breed) breed.getSelectedItem();
                  if (b == null) throw new IllegalArgumentException("Choose a breed.");
                  PetInput input =
                      new PetInput(
                          name.getText(),
                          b.id(),
                          Validation.birthDate(birth.getText()),
                          (String) sex.getSelectedItem(),
                          description.getText(),
                          image[0],
                          Validation.money(fee.getText()),
                          (PetStatus) status.getSelectedItem());
                  Ui.work(
                      d,
                      save,
                      () ->
                          service.save(
                              session,
                              pet == null ? null : pet.id(),
                              pet == null ? 0 : pet.version(),
                              input),
                      id -> {
                        d.dispose();
                        reload.run();
                      });
                }));
    d.setVisible(true);
  }
}
