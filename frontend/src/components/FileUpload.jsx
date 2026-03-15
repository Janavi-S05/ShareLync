import React, { useEffect, useState } from "react";
import { Alert, Box, Button, Container, Typography } from "@mui/material";
import { useCookies } from "react-cookie";
import { AlertDialog } from "../dialogs/AlertDialog";

export const FileUpload = (props) => {
    
    const { setFileUploaded } = props;
    const [file, setFile] = useState(null);
    const [cookies] = useCookies(['user']);

    const [username, setUsername] = useState("");
    const [alertMessage, setAlertMessage] = useState("");
    const [alertHeader, setAlertHeader] = useState("");
    const [alertOpen, setAlertOpen] = useState(false);
    const [uploadBtnText, setUploadBtnText] = useState("Upload File");
    const [uploadDisabled, setUploadDisabled] = useState(false);
    
    const user = React.useMemo(() => {
        if (cookies.user && typeof cookies.user === 'string') {
            try {
                return JSON.parse(cookies.user);
            } catch (e) { return null; }
        }
        return cookies.user;
    }, [cookies.user]);

    useEffect(() => {
        if (user) {
            setUsername(user.username);
        }
    }, [user]);

    const onFileSelect = (event) => {
        setFile(event.target.files[0]);
    }
    const uploadFile = (e) => {
        e.preventDefault();

        if (!user || !file) {
            console.error("User not logged in or no file selected");
            return;
        }

        setUploadBtnText("Uploading File...");
        setUploadDisabled(true);

        const formData = new FormData();
        formData.append("file", file);
        formData.append("username", user.username);

        fetch(`/api/file/upload`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${user.token}`,
            },
            body: formData,
        })
            .then(resp => resp.json())
            .then(data => {
                setFile(null);
                document.getElementById("upload-file-input").value = "";
                if (data.status > 299) {
                    setAlertHeader("Error uploading");
                    setAlertMessage(data.message);
                    setAlertOpen(true);
                }
                setUploadBtnText("Upload File");
                setUploadDisabled(false);
                setFileUploaded(true);
            })
            .catch(err => {
                console.error("Error uploading file ", err);
                setUploadBtnText("Upload File");
                setUploadDisabled(false);
            });
    }
    const handleAlertClose = () => {
        setAlertHeader("");
        setAlertMessage("");
        setAlertOpen(false);
    }
    return (
        <Container component='div'>
            <Typography sx={{ marginBottom: 2 }} variant="h5">File Upload</Typography>
            <form onSubmit={uploadFile} id="uploadDocument" style={{
                display: 'flex',
                justifyContent: "flex-start",
                alignContent: "center",
                flexDirection: "column",
                marginTop: 4
            }}>
                <Box sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    marginLeft: 'auto',
                    marginRight: 'auto',
                }}>
                    {user ?
                        <>
                            <input id="upload-file-input" type="file" name="file" onChange={onFileSelect} />
                            <Button sx={{ marginTop: 1 }}
                                size="small"
                                variant="contained"
                                disabled={uploadDisabled || !file}
                                type="submit">
                                {uploadBtnText} </Button>
                        </>
                        :
                        <Typography>Please log in to upload files.</Typography>
                    }
                </Box>
                <Box sx={{
                    marginTop: 2, 
                    opacity: 0.5,
                    marginLeft: "auto",
                    marginRight: "auto"
                }}>
                    <Alert variant="outlined" severity="info">
                        Only [".pdf", ".doc", ".docx", ".txt", ".md", ".json"] file types are supported so far
                    </Alert>
                </Box>
            </form>

            <AlertDialog open={alertOpen}
                handleClose={handleAlertClose}
                title={alertHeader}
                content={alertMessage} />

        </Container>
    )
}