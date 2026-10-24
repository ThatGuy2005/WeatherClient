package org.example.idojaras.ui;

/**
 * What the presenter needs from the view. Implemented by the Swing window, and mocked in
 * tests so the presenter can be tested without a GUI.
 *
 * <p>All methods are called on the Swing event dispatch thread.
 */
public interface IdojarasNezet {

    /** Enables or disables the Refresh button (disabled while a request is running). */
    void setFrissitesEngedelyezve(boolean engedelyezve);

    /** Shows a successful result; all values are already formatted for display. */
    void eredmenytMutat(String varos, String homerseklet, String szel, String idopont);

    /** Reports an error to the user. */
    void hibatMutat(String uzenet);
}
