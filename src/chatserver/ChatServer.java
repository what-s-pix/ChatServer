/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package chatserver;

import db.Conexion; // Importa tu clase Conexion
import java.sql.Connection;
import java.sql.SQLException;
public class ChatServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Servidor();
    
    }
    
}
