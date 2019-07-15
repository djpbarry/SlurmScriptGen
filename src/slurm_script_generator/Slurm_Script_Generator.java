/*
 * Copyright (C) 2019 David Barry <david.barry at crick dot ac dot uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package slurm_script_generator;

import IO.BioFormats.BioFormatsFileLister;
import IO.BioFormats.BioFormatsImg;
import UtilClasses.GenUtils;
import UtilClasses.GenVariables;
import ij.IJ;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import loci.formats.FormatException;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Slurm_Script_Generator {

    private final String inputLocation;
    private final String outputLocation;
    private final String gianiJarLocation;
    private final String propFileLocation;

    public Slurm_Script_Generator() {
        this(null, null, null, null);
    }

    public Slurm_Script_Generator(String input, String output, String gianiJarLocation, String propFileLocation) {
        this.inputLocation = input;
        this.outputLocation = output;
        this.gianiJarLocation = gianiJarLocation;
        this.propFileLocation = propFileLocation;
    }

    public void run() {
        File inputDir = new File(inputLocation);
        if (!inputDir.isDirectory()) {
            IJ.log("Input is not a directory - aborting.");
            return;
        }
        File scriptFile = new File(String.format("%s%sGiani_Slurm_Script.sh", inputDir, File.separator));
        File jobListFile = new File(String.format("%s%sGiani_Job_List.txt", inputDir, File.separator));
        Writer scriptWriter;
        Writer jobListWriter;
        try {
            if (!createFile(scriptFile) || !createFile(jobListFile)) {
                return;
            }
            scriptWriter = new OutputStreamWriter(new FileOutputStream(scriptFile), GenVariables.ISO);
            jobListWriter = new OutputStreamWriter(new FileOutputStream(jobListFile), GenVariables.ISO);
            int jobCount = 0;

            buildJobList(inputDir, jobCount, jobListWriter);

            scriptWriter.write("#!/bin/bash\n\n");
            scriptWriter.write("#SBATCH --job-name=fiji-giani\n");
//            scriptWriter.write("#SBATCH --ntasks=1\n");
            scriptWriter.write("#SBATCH --time=1:00:00\n");
//            scriptWriter.write("#SBATCH --mem-per-cpu=128G\n");
            scriptWriter.write("#SBATCH --cpus-per-task=16\n");
            scriptWriter.write(String.format("#SBATCH --array=0-%d\n\n", jobCount - 1));
//            scriptWriter.write("#SBATCH --partition=hmem\n");
//            scriptWriter.write("#SBATCH --output=/home/camp/barryd/working/barryd/hpc/output/res.txt\n\n");
            scriptWriter.write("ml Java/1.9.0.4\n");
            scriptWriter.write(String.format("srun --output=%s/giani_log_ID_$SLURM_ARRAY_TASK_ID.txt java -jar %s %s %s $SLURM_ARRAY_TASK_ID\n",
                    outputLocation, gianiJarLocation, jobListFile.getAbsolutePath(), propFileLocation));

            scriptWriter.close();
            jobListWriter.close();
        } catch (IOException e) {
            IJ.log(String.format("Encountered a problem generating %s - aborting.", inputDir.getAbsolutePath()));
        }
        IJ.log("Done.");
    }

    private boolean createFile(File file) throws IOException {
        if (file.exists()) {
            if (!file.delete()) {
                IJ.log(String.format("Could not delete %s - aborting.", file.getAbsolutePath()));
                return false;
            }
        } else if (!file.createNewFile()) {
            IJ.log(String.format("Could not create %s - aborting.", file.getAbsolutePath()));
            return false;
        }
        return true;
    }

    private void buildJobList(File inputDir, int jobCount, Writer jobListWriter) throws IOException {
        File[] files = inputDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                buildJobList(file, jobCount, jobListWriter);
                ArrayList<String> validFiles = BioFormatsFileLister.obtainValidFileList(file);
                for (String f : validFiles) {
                    BioFormatsImg img = new BioFormatsImg();
                    try {
                        img.setId(String.format("%s%s%s", file.getAbsolutePath(), File.separator, f));
                    } catch (IOException | FormatException e) {
                        GenUtils.logError(e, String.format("Failed to initialise %s", f));
                    }
                    int nSeries = img.getSeriesCount();
                    for (int s = 0; s < nSeries; s++) {
                        jobListWriter.write(String.format("%d, %s, %d\n", jobCount++, img.getId(), s));
                    }
                }
            }
        }
    }
}
