package sales_inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.*;

public class GestionProduitsCommandes extends JFrame {
    // Déclarations des champs et des boutons
    private JTextField tfProductIdToDelete;
    private JButton btnDeleteProduct;
    private JButton btnShowAddProductDialog, btnShowAddOrderDialog;
    private JTable tableStock;
    private DefaultTableModel tableModel;

    // Informations de connexion à la base de données
    private static final String URL = "jdbc:postgresql://localhost:5432/SalesInventoryDB";
    private static final String USER = "postgres";
    private static final String PASSWORD = "UDEV-2";

    // Constructeur de la classe GestionProduitsCommandes
    public GestionProduitsCommandes() {
        // Configuration de la fenêtre principale
        setTitle("Gestion des produits et des commandes");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Création du panel d'action en bas de la fenêtre
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        add(actionPanel, BorderLayout.SOUTH);

        // Ajout des boutons au panel d'action
        btnShowAddProductDialog = new JButton("Nouveau Produit");
        btnShowAddProductDialog.setPreferredSize(new Dimension(150, 30));
        actionPanel.add(btnShowAddProductDialog);

        btnShowAddOrderDialog = new JButton("Nouvelle Commande");
        btnShowAddOrderDialog.setPreferredSize(new Dimension(150, 30));
        actionPanel.add(btnShowAddOrderDialog);

        btnDeleteProduct = new JButton("Supprimer un Produit");
        btnDeleteProduct.setPreferredSize(new Dimension(150, 30));
        actionPanel.add(btnDeleteProduct);

        // Configuration du tableau pour afficher les produits en stock
        tableModel = new DefaultTableModel(new Object[]{"ID", "Nom", "Prix", "Quantité",
        		"Désignation", "Catégorie", "Dernière Mise à Jour"}, 0);
        tableStock = new JTable(tableModel);
        add(new JScrollPane(tableStock), BorderLayout.CENTER);

        // Ajout des listeners d'événements aux boutons
        btnDeleteProduct.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDeleteProductDialog();
            }
        });

        btnShowAddProductDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAddProductDialog();
            }
        });

        btnShowAddOrderDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAddOrderDialog();
            }
        });

        // Chargement des données de stock initiales
        loadStockData();
    }

    // Méthode pour se connecter à la base de données
    private Connection connectToDatabase() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Méthode pour afficher le dialogue d'ajout de produit
    private void showAddProductDialog() {
        JDialog addProductDialog = new JDialog(GestionProduitsCommandes.this, "Ajouter un produit", true);
        addProductDialog.setSize(300, 300);
        addProductDialog.setLayout(new GridLayout(6, 2, 5, 5));
        addProductDialog.setLocationRelativeTo(GestionProduitsCommandes.this);

        // Champs de saisie pour les informations du produit
        JTextField tfNomDialog = new JTextField();
        JTextField tfDesignationDialog = new JTextField();
        JTextField tfPrixDialog = new JTextField();
        JTextField tfCategorieDialog = new JTextField();
        JTextField tfQuantiteDialog = new JTextField();
        JButton btnAddProductDialog = new JButton("Ajouter");

        // Ajout des composants au dialogue
        addProductDialog.add(new JLabel("Nom:"));
        addProductDialog.add(tfNomDialog);
        addProductDialog.add(new JLabel("Désignation:"));
        addProductDialog.add(tfDesignationDialog);
        addProductDialog.add(new JLabel("Prix:"));
        addProductDialog.add(tfPrixDialog);
        addProductDialog.add(new JLabel("Catégorie:"));
        addProductDialog.add(tfCategorieDialog);
        addProductDialog.add(new JLabel("Quantité:"));
        addProductDialog.add(tfQuantiteDialog);
        addProductDialog.add(new JLabel(""));
        addProductDialog.add(btnAddProductDialog);

        // validation de l'ajout de produit
        btnAddProductDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                // Vérification des champs non vides
                if (tfNomDialog.getText().trim().isEmpty() ||
                    tfDesignationDialog.getText().trim().isEmpty() ||
                    tfPrixDialog.getText().trim().isEmpty() ||
                    tfCategorieDialog.getText().trim().isEmpty() ||
                    tfQuantiteDialog.getText().trim().isEmpty()) 
                {
                    JOptionPane.showMessageDialog(null, "Veuillez remplir tous les champs !");
                    return;
                }

                // Vérification des formats des valeurs numériques
                try {
                    new BigDecimal(tfPrixDialog.getText());
                    Integer.parseInt(tfQuantiteDialog.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Veuillez entrer des valeurs valides !");
                    return;
                }

                // Ajout du produit dans la base de données
                try (Connection conn = connectToDatabase()) {
                    String sql = "INSERT INTO produits (nom, designation, prix, categorie, LastUpdated) VALUES"
                    		+ " (?, ?, ?, ?, CURRENT_TIMESTAMP)";
                    try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        pst.setString(1, tfNomDialog.getText());
                        pst.setString(2, tfDesignationDialog.getText());
                        pst.setBigDecimal(3, new BigDecimal(tfPrixDialog.getText()));
                        pst.setString(4, tfCategorieDialog.getText());
                        pst.executeUpdate();

                        // Récupération de l'ID du produit ajouté
                        ResultSet generatedKeys = pst.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            int productId = generatedKeys.getInt(1);
                            // Ajout du stock associé au produit
                            String stockSql = "INSERT INTO stock (produit_id, quantite, LastUpdated) VALUES "
                            		+ "(?, ?, CURRENT_TIMESTAMP)";
                            try (PreparedStatement stockPst = conn.prepareStatement(stockSql)) {
                                stockPst.setInt(1, productId);
                                stockPst.setInt(2, Integer.parseInt(tfQuantiteDialog.getText()));
                                stockPst.executeUpdate();
                            }
                        }
                        JOptionPane.showMessageDialog(null, "Produit ajouté avec succès !");
                        // Recharge les données de stock
                        loadStockData();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Erreur lors de l'ajout du produit : " + ex.getMessage());
                }
                addProductDialog.dispose();
            }
        });

        addProductDialog.setVisible(true);
    }

    // Méthode pour afficher le dialogue d'ajout de commande
    private void showAddOrderDialog() {
        JDialog addOrderDialog = new JDialog(GestionProduitsCommandes.this, "Ajouter une commande", true);
        addOrderDialog.setSize(300, 200);
        addOrderDialog.setLayout(new GridLayout(5, 2, 5, 5));
        addOrderDialog.setLocationRelativeTo(GestionProduitsCommandes.this);

        // Champs de saisie pour les informations de la commande
        JTextField tfProductIdForOrderDialog = new JTextField();
        JTextField tfQuantiteDialog = new JTextField();
        JTextField tfPrixTotalDialog = new JTextField();
        JButton btnAddOrderDialog = new JButton("Ajouter");

        // Ajout des composants au dialogue
        addOrderDialog.add(new JLabel("ID Produit:"));
        addOrderDialog.add(tfProductIdForOrderDialog);
        addOrderDialog.add(new JLabel("Quantité:"));
        addOrderDialog.add(tfQuantiteDialog);
        addOrderDialog.add(new JLabel("Prix Total:"));
        addOrderDialog.add(tfPrixTotalDialog);
        addOrderDialog.add(new JLabel(""));
        addOrderDialog.add(btnAddOrderDialog);

        // Validation de l'ajout de commande
        btnAddOrderDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                // Vérification des champs non vides
                if (tfProductIdForOrderDialog.getText().trim().isEmpty() ||
                    tfQuantiteDialog.getText().trim().isEmpty() ||
                    tfPrixTotalDialog.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Veuillez remplir tous les champs !");
                    return;
                }

                // Vérification des formats des valeurs numériques
                try {
                    Integer.parseInt(tfProductIdForOrderDialog.getText());
                    Integer.parseInt(tfQuantiteDialog.getText());
                    new BigDecimal(tfPrixTotalDialog.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Veuillez entrer des valeurs numériques valides !");
                    return;
                }

                // Ajout de la commande dans la base de données
                try (Connection conn = connectToDatabase()) {
                    String sql = "INSERT INTO ventes (produit_id, quantite_vendue, prix_total) VALUES (?, ?, ?)";
                    try (PreparedStatement pst = conn.prepareStatement(sql)) {
                        pst.setInt(1, Integer.parseInt(tfProductIdForOrderDialog.getText()));
                        pst.setInt(2, Integer.parseInt(tfQuantiteDialog.getText()));
                        pst.setBigDecimal(3, new BigDecimal(tfPrixTotalDialog.getText()));
                        pst.executeUpdate();
                        JOptionPane.showMessageDialog(null, "Commande ajoutée avec succès !");
                        // Recharge les données de stock
                        loadStockData();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Erreur lors de l'ajout de la commande : " + ex.getMessage());
                }
                addOrderDialog.dispose();
            }
        });

        addOrderDialog.setVisible(true);
    }

    // Méthode pour afficher le dialogue de suppression de produit
    private void showDeleteProductDialog() {
        JDialog deleteProductDialog = new JDialog(GestionProduitsCommandes.this, "Supprimer un produit", true);
        deleteProductDialog.setSize(300, 150);
        deleteProductDialog.setLayout(new GridLayout(3, 2, 5, 5));
        deleteProductDialog.setLocationRelativeTo(GestionProduitsCommandes.this);

        // Champs de saisie pour l'ID du produit à supprimer
        tfProductIdToDelete = new JTextField();
        JButton btnDeleteProductDialog = new JButton("OK");
        JButton btnCancelDialog = new JButton("Annuler");

        // Ajout des composants au dialogue
        deleteProductDialog.add(new JLabel("ID Produit:"));
        deleteProductDialog.add(tfProductIdToDelete);
        deleteProductDialog.add(new JLabel(""));
        deleteProductDialog.add(btnDeleteProductDialog);
        deleteProductDialog.add(new JLabel(""));
        deleteProductDialog.add(btnCancelDialog);

        // Validation de la suppression de produit
        btnDeleteProductDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Vérification du champ non vide
                if (tfProductIdToDelete.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Veuillez entrer l'ID du produit !");
                    return;
                }

                // Vérification du format de l'ID
                try {
                    Integer.parseInt(tfProductIdToDelete.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Veuillez entrer un ID valide !");
                    return;
                }

                // Suppression du produit dans la base de données
                try (Connection conn = connectToDatabase()) {
                    String sql = "DELETE FROM produits WHERE id = ?";
                    try (PreparedStatement pst = conn.prepareStatement(sql)) {
                        pst.setInt(1, Integer.parseInt(tfProductIdToDelete.getText()));
                        int affectedRows = pst.executeUpdate();
                        if (affectedRows > 0) {
                            JOptionPane.showMessageDialog(null, "Produit supprimé avec succès !");
                            // Recharge les données de stock
                            loadStockData();
                        } else {
                            JOptionPane.showMessageDialog(null, "Aucun produit trouvé avec cet ID !");
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Erreur lors de la suppression du produit : " + ex.getMessage());
                }
                deleteProductDialog.dispose();
            }
        });

        // Annulation de la suppression
        btnCancelDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteProductDialog.dispose();
            }
        });

        deleteProductDialog.setVisible(true);
    }

    // Méthode pour charger les données de stock depuis la base de données
    private void loadStockData() {
        tableModel.setRowCount(0);
        String sql = "SELECT p.id, p.nom, p.prix, s.quantite, p.designation, p.categorie, s.lastupdated " +
                     "FROM produits p " +
                     "JOIN stock s ON p.id = s.produit_id";

        try (Connection conn = connectToDatabase();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Parcours des résultats de la requête pour les ajouter au tableau
            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                BigDecimal prix = rs.getBigDecimal("prix");
                int quantite = rs.getInt("quantite");
                String designation = rs.getString("designation");
                String categorie = rs.getString("categorie");
                Timestamp lastUpdated = rs.getTimestamp("lastupdated");

                tableModel.addRow(new Object[]{id, nom, prix, quantite, designation, categorie, lastUpdated});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des données de stock : " + ex.getMessage());
        }
    }

    // Méthode main pour lancer l'application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new GestionProduitsCommandes().setVisible(true);
            }
        });
    }
}
