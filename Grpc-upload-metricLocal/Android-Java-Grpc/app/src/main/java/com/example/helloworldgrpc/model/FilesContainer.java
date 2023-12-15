package com.example.helloworldgrpc.model;

import java.io.File;

public class FilesContainer {
    private File file;
    private long filesize;
    private int indextoupdate;

    public FilesContainer(File file, long filesize, int indextoupdate) {
        this.file = file;
        this.filesize = filesize;
        this.indextoupdate = indextoupdate;
    }


    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public int getIndextoupdate() {
        return indextoupdate;
    }

    public void setIndextoupdate(int indextoupdate) {
        this.indextoupdate = indextoupdate;
    }
}
