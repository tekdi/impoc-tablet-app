package com.example.helloworldgrpc.activity;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.helloworldgrpc.Chunk;
import com.example.helloworldgrpc.model.FilesContainer;
import com.example.helloworldgrpc.GreeterGrpc;
import com.example.helloworldgrpc.HelloReply;
import com.example.helloworldgrpc.HelloRequest;
import com.example.helloworldgrpc.R;
import com.example.helloworldgrpc.Reply;
import com.example.helloworldgrpc.adapter.CustomAdapter;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class UploadFiles extends AppCompatActivity {
    long filesize = 0;
    File file;
    ManagedChannel channel = null;
    //String serverIp = "172.132.45.176";
    String serverIp = "3.110.143.240";


    int portToConnect = 50051;
    ProgressBar pbProgress;
    List<String> alFilesToUploadList = new ArrayList<>();
    List<Integer> alUploadedFilesStatus = new ArrayList<>();
    List<FilesContainer> alFilesContainer = new ArrayList<>();
    private int indextoUpdate = -1;

    ListView rcvFiles;
    CustomAdapter adapter;
    private int STORAGE_PERMISSION_CODE = 200;

    TextView frameEmpty;
    List<String> alFileSizesFromGrpc = new ArrayList<>();


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_files);

        pbProgress = findViewById(R.id.pb_progress);
        rcvFiles = findViewById(R.id.rcv_files);
        frameEmpty = findViewById(R.id.frame_empty);

        this.setTitle("Grpc Upload");

        channel = ManagedChannelBuilder.forAddress(serverIp, portToConnect)
                .usePlaintext()
                .build();


        if (isStoragePermissionGranted()) {
            checkForUploads();
        }
    }

    public boolean checkForServerConnection(){
        try {
            GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
            HelloRequest request = HelloRequest.newBuilder().setName("Hi").build();
            HelloReply reply = stub.sayHello(request);
            if(!reply.getMessage().isEmpty()) {
                Toast.makeText(this, "Server is Up "+reply.getMessage(), Toast.LENGTH_SHORT).show();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Please check the server", Toast.LENGTH_SHORT).show();
            //return String.format("Failed... : %n%s", sw);
            return false;
        }
    }

    public void checkForUploads() {
        pbProgress.setVisibility(View.VISIBLE);
        boolean serverConnection = checkForServerConnection();

        do{
            Toast.makeText(this , "Waiting for connection", Toast.LENGTH_SHORT).show();

            if(serverConnection) {
                Toast.makeText(this , "Channel Ready", Toast.LENGTH_SHORT).show();
                if(!channel.isTerminated() && !channel.isShutdown()){
                    pbProgress.setVisibility(View.GONE);
                    filesLooper();
                    break;
                }
            } else {
                Toast.makeText(this , "Server is down", Toast.LENGTH_SHORT).show();
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkForServerConnection();
                }
            }, 2000);

        }while(true);


    }

    private boolean isStoragePermissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
                checkForUploads();
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void filesLooper() {

        String folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/FilesToUpload";
        String folderMovePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/UploadedFiles";

        File directory = new File(folderPath);
        if(!directory.exists()) {
            if (!directory.mkdirs()) {
// Directory creation failed
                Toast.makeText(this, "Folder creation failed", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File directoryUploaded = new File(folderMovePath);
        if(!directoryUploaded.exists()) {
            if (!directoryUploaded.mkdirs()) {
// Directory creation failed
                Toast.makeText(this, "Folder creation failed", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File[] listOfFiles = directory.listFiles();

        if (listOfFiles.length > 0) {

            rcvFiles.setVisibility(View.VISIBLE);
            frameEmpty.setVisibility(View.GONE);

            for (int j = 0; j <= listOfFiles.length - 1; j++) {
                alFilesToUploadList.add(listOfFiles[j].getName());
                alUploadedFilesStatus.add(0);
            }

            adapter = new CustomAdapter(this, alFilesToUploadList, alUploadedFilesStatus);
            rcvFiles.setAdapter(adapter);
            adapter.notifyDataSetChanged();

            for (int i = 0; i <= listOfFiles.length - 1; i++) {
                pbProgress.setVisibility(View.VISIBLE);

                file = new File(folderPath + "/" + listOfFiles[i].getName());
                indextoUpdate = i;

                filesize = getFileSize(file);

                FilesContainer fileContainer = new FilesContainer(
                        file,
                        filesize,
                        i
                );

                alFilesContainer.add(fileContainer);

                Log.d("GRPC_TEST", "Returned file size local " + filesize + " Filename " + listOfFiles[i].getName());

                if(i == listOfFiles.length-1){
                    fileUploadClient(file, true, i);
                } else {
                    fileUploadClient(file, false, i);
                }

            }

        } else {

            frameEmpty.setVisibility(View.VISIBLE);
            rcvFiles.setVisibility(View.GONE);

        }
    }


    public void fileUploadClient(File fileInput, boolean stopProgressBar, int indextoupdate) {

        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);

        StreamObserver<Chunk> requestObserver = stub.uploadFile(new StreamObserver<Reply>() {
            @Override
            public void onNext(Reply value) {
                if (!value.getMessage().isEmpty()) {
                    System.out.println("GRPC_TEST File upload Data " + value.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alFileSizesFromGrpc.add(value.getMessage());
                                for(int i = 0; i <= alFilesContainer.size()-1; i++){
                                    if (Long.parseLong(value.getMessage()) == alFilesContainer.get(i).getFilesize() && fileInput.getName().equals(alFilesContainer.get(i).getFile().getName())) {
                                        alUploadedFilesStatus.add(alFilesContainer.get(i).getIndextoupdate(), 1);
                                        adapter.notifyDataSetChanged();
                                        deleteFile(alFilesContainer.get(i).getFile());
                                        break;
                                    }
                                }

                            }
                        });
                } else {
                    System.out.println("GRPC_TEST File upload failed");
                }
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbProgress.setVisibility(View.GONE);
                    }
                });
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(stopProgressBar) {
                            pbProgress.setVisibility(View.GONE);
                        }
                    }
                });
                if(stopProgressBar) {
                    channel.shutdown();
                }
            }
        });


        String filename = fileInput.getName();
        byte[] byteConvertedFilename = filename.getBytes(StandardCharsets.UTF_8);

        Chunk firstChunk = Chunk.newBuilder()
                .setBuffer(ByteString.copyFrom(byteConvertedFilename))
                .build();
        requestObserver.onNext(firstChunk);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                Chunk chunk = Chunk.newBuilder()
                        .setBuffer(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead))
                        .build();
                requestObserver.onNext(chunk);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        requestObserver.onCompleted();

    }

    public static long getFileSize(File file) {
        if (file.exists()) {
            return file.length();
        } else {
            return 0;
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());

            if (sourceFile.delete()) {
                Log.d("GRPC_TEST", "File deleted successfully " + sourceFile.getName());
            } else {
                Log.d("GRPC_TEST", "Problem in File delete " + sourceFile.getName());
            }

        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            String folderMovePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/UploadedFiles";

            File fileToMove = new File(folderMovePath+ "/" +file.getName());
            try
            {
                copyFile(file, fileToMove);

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

}