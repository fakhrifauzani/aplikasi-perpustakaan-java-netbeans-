package perpustakaan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

// --- CLASS LOGIN ---
class LoginUI extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;

    public LoginUI() {
        setTitle("Login Perpustakaan");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 1, 10, 10));

        JPanel panelInput = new JPanel(new GridLayout(2, 2, 5, 5));
        panelInput.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        panelInput.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        panelInput.add(txtUsername);

        panelInput.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        panelInput.add(txtPassword);

        add(panelInput);

        JPanel panelTombol = new JPanel();
        JButton btnLogin = new JButton("Login");
        panelTombol.add(btnLogin);
        add(panelTombol);

        // Logika Login ke Database
        btnLogin.addActionListener(e -> {
            try {
                String sql = "SELECT * FROM users WHERE username=? AND password=?";
                Connection conn = Koneksi.configDB();
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, txtUsername.getText());
                pst.setString(2, new String(txtPassword.getPassword()));
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    String role = rs.getString("role");
                    JOptionPane.showMessageDialog(this, "Login Berhasil sebagai " + role.toUpperCase() + "!");
                    new PerpustakaanApp(role).setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Username atau Password salah!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error Database: " + ex.getMessage());
            }
        });
    }
}

// --- CLASS APLIKASI UTAMA ---
public class PerpustakaanApp extends JFrame {
    
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtJudul, txtPenulis;
    private String role;

    public PerpustakaanApp(String roleUser) {
        this.role = roleUser;

        setTitle("Perpustakaan - Login sebagai: " + role.toUpperCase());
        setSize(600, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- PANEL INPUT BUKU ---
        JPanel panelInput = new JPanel(new GridLayout(3, 2, 5, 5));
        panelInput.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        panelInput.add(new JLabel("Judul Buku:"));
        txtJudul = new JTextField();
        panelInput.add(txtJudul);

        panelInput.add(new JLabel("Penulis:"));
        txtPenulis = new JTextField();
        panelInput.add(txtPenulis);

        JButton btnTambah = new JButton("Tambah Buku");
        panelInput.add(new JLabel("")); 
        panelInput.add(btnTambah);
        
        if (role.equals("petugas")) {
            add(panelInput, BorderLayout.NORTH);
        } else {
            JLabel sapaan = new JLabel("Selamat datang! Silakan pilih buku di tabel.", SwingConstants.CENTER);
            sapaan.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            add(sapaan, BorderLayout.NORTH);
        }

        // --- TABEL DATA BUKU ---
        String[] kolom = {"ID", "Judul", "Penulis", "Status"};
        tableModel = new DefaultTableModel(kolom, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);
        loadDataBuku(); 

        // --- PANEL AKSI ---
        JPanel panelAksi = new JPanel();
        JButton btnPinjam = new JButton("Pinjam Buku");
        JButton btnKembali = new JButton("Kembalikan Buku");
        JButton btnLogout = new JButton("Logout");
        
        panelAksi.add(btnPinjam);
        panelAksi.add(btnKembali);
        panelAksi.add(btnLogout);
        add(panelAksi, BorderLayout.SOUTH);

        // --- EVENT LISTENERS ---
        btnTambah.addActionListener(e -> tambahBuku());
        btnPinjam.addActionListener(e -> ubahStatusBuku(1)); 
        btnKembali.addActionListener(e -> ubahStatusBuku(0)); 
        
        btnLogout.addActionListener(e -> {
            new LoginUI().setVisible(true);
            this.dispose();
        });
    }

    private void tambahBuku() {
        String judul = txtJudul.getText();
        String penulis = txtPenulis.getText();
        
        if (!judul.trim().isEmpty() && !penulis.trim().isEmpty()) {
            try {
                String sql = "INSERT INTO buku (judul, penulis, status_pinjam) VALUES (?, ?, 0)";
                Connection conn = Koneksi.configDB();
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, judul);
                pst.setString(2, penulis);
                pst.execute();
                
                JOptionPane.showMessageDialog(this, "Buku berhasil ditambahkan!");
                loadDataBuku(); 
                txtJudul.setText("");
                txtPenulis.setText("");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal tambah buku: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Isi judul dan penulisnya dulu!");
        }
    }

    private void ubahStatusBuku(int statusBaru) {
        int row = table.getSelectedRow(); 
        if (row >= 0) {
            String idBuku = tableModel.getValueAt(row, 0).toString();
            String statusSaatIni = tableModel.getValueAt(row, 3).toString();
            
            if ((statusBaru == 1 && statusSaatIni.equals("Dipinjam")) || 
                (statusBaru == 0 && statusSaatIni.equals("Tersedia"))) {
                JOptionPane.showMessageDialog(this, "Cek lagi, buku ini udah dalam status tersebut!");
                return;
            }

            try {
                String sql = "UPDATE buku SET status_pinjam = ? WHERE id = ?";
                Connection conn = Koneksi.configDB();
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, statusBaru);
                pst.setString(2, idBuku);
                pst.execute();
                
                loadDataBuku(); 
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal update status: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Klik salah satu buku di tabel dulu!");
        }
    }

    private void loadDataBuku() {
        tableModel.setRowCount(0); 
        try {
            String sql = "SELECT * FROM buku";
            Connection conn = Koneksi.configDB();
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            while (rs.next()) {
                String id = rs.getString("id");
                String judul = rs.getString("judul");
                String penulis = rs.getString("penulis");
                int status = rs.getInt("status_pinjam");
                String teksStatus = (status == 1) ? "Dipinjam" : "Tersedia";
                
                tableModel.addRow(new Object[]{id, judul, penulis, teksStatus});
            }
        } catch (Exception e) {
            System.err.println("Gagal load data: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }
}