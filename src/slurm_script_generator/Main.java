/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package slurm_script_generator;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        (new Slurm_Script_Generator(args[0], args[1], args[2], args[3])).run();
        System.exit(0);
    }

}
