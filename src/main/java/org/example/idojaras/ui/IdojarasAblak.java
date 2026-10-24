package org.example.idojaras.ui;

import org.example.idojaras.domain.IdojarasSzolgaltatas;
import org.example.idojaras.domain.Varos;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * The main window: city selector, Refresh button and result labels.
 *
 * <p>It is a passive view: it forwards clicks to the {@link IdojarasPresenter} and displays
 * whatever the presenter tells it to. It only knows the {@link IdojarasSzolgaltatas}
 * interface, never a concrete implementation, and nothing about HTTP.
 */
public final class IdojarasAblak extends JFrame implements IdojarasNezet {

    private static final String URES = "—";

    private final JComboBox<Varos> varosValaszto;
    private final JButton frissitesGomb = new JButton("Refresh");
    private final JLabel varosErtek = ertekCimke();
    private final JLabel homersekletErtek = ertekCimke();
    private final JLabel szelErtek = ertekCimke();
    private final JLabel idopontErtek = ertekCimke();
    private final JLabel allapot = new JLabel(" ");
    private IdojarasPresenter presenter;

    /**
     * Creates, wires and shows the window, then loads the weather for the first city.
     * Must be called on the event dispatch thread.
     */
    public static IdojarasAblak megnyit(IdojarasSzolgaltatas szolgaltatas, List<Varos> varosok, String cim) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("IdojarasAblak must be created on the event dispatch thread.");
        }
        IdojarasAblak ablak = new IdojarasAblak(varosok, cim);
        ablak.presenter = new IdojarasPresenter(szolgaltatas, ablak, new SwingWorkerFuttato());
        ablak.pack();
        ablak.setLocationRelativeTo(null);
        ablak.setVisible(true);
        ablak.frissit();
        return ablak;
    }

    private IdojarasAblak(List<Varos> varosok, String cim) {
        super(cim);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        varosValaszto = new JComboBox<>(varosok.toArray(Varos[]::new));
        frissitesGomb.addActionListener(e -> frissit());

        JPanel vezerlok = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        vezerlok.add(new JLabel("City:"));
        vezerlok.add(varosValaszto);
        vezerlok.add(frissitesGomb);

        JPanel eredmeny = new JPanel(new GridLayout(0, 2, 12, 8));
        eredmeny.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Current weather"),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        eredmeny.add(new JLabel("Location:"));
        eredmeny.add(varosErtek);
        eredmeny.add(new JLabel("Temperature:"));
        eredmeny.add(homersekletErtek);
        eredmeny.add(new JLabel("Wind speed:"));
        eredmeny.add(szelErtek);
        eredmeny.add(new JLabel("Time (local):"));
        eredmeny.add(idopontErtek);

        JPanel tartalom = new JPanel(new BorderLayout(0, 10));
        tartalom.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tartalom.add(vezerlok, BorderLayout.NORTH);
        tartalom.add(eredmeny, BorderLayout.CENTER);
        tartalom.add(allapot, BorderLayout.SOUTH);
        setContentPane(tartalom);
        setMinimumSize(new Dimension(400, 0));
        getRootPane().setDefaultButton(frissitesGomb);
    }

    private void frissit() {
        presenter.frissit((Varos) varosValaszto.getSelectedItem());
    }

    @Override
    public void setFrissitesEngedelyezve(boolean engedelyezve) {
        frissitesGomb.setEnabled(engedelyezve);
        allapot.setText(engedelyezve ? " " : "Loading…");
        setCursor(engedelyezve ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    @Override
    public void eredmenytMutat(String varos, String homerseklet, String szel, String idopont) {
        varosErtek.setText(varos);
        homersekletErtek.setText(homerseklet);
        szelErtek.setText(szel);
        idopontErtek.setText(idopont);
    }

    @Override
    public void hibatMutat(String uzenet) {
        JOptionPane.showMessageDialog(this, uzenet, "Weather unavailable", JOptionPane.ERROR_MESSAGE);
    }

    private static JLabel ertekCimke() {
        JLabel cimke = new JLabel(URES);
        cimke.setFont(cimke.getFont().deriveFont(Font.BOLD));
        return cimke;
    }
}
