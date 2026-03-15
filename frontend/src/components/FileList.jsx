import React, { useContext, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, Container, Dialog, DialogActions, DialogContent, DialogTitle, Divider, TextField, Typography } from "@mui/material";
import { useCookies } from "react-cookie";
import { API_FILE_PATH } from "../Constants";
import { AlertDialog } from "../dialogs/AlertDialog";
import { DateTimeRange } from "./DateRangeSelection";
import "./components.css";
import { ScheduleContext } from "./ScheduleContext";

export const FileList = (props) => {
    // This is the correct FileList component
    const { fileUploadDone, setFileUploadDone } = props;
    const { schedule } = useContext(ScheduleContext);

    const [cookies] = useCookies(['user']);
    // ... (the rest of the correct FileList code follows)
    const [userFiles, setUserFiles] = useState([]);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [scheduleDialogOpen, setScheduleDialogOpen] = useState(false);
    const [fileToShare, setFileToShare] = useState("");
    const [showFiles, setShowFiles] = useState(false);
    const [alertOpen, setAlertOpen] = useState(false);
    const [notLoginErr, setNotLoginError] = useState(false);
    const [alertHeader, setAlertHeader] = useState("");
    const [alertMessage, setAlertMessage] = useState("");
    const [shareBtnTxt, setShareBtnTxt] = useState("Share");
    const [shareBtnDisabled, setShareBtnDisabled] = useState(false);
    const scheduleContext = useContext(ScheduleContext);
    const [selSchedule, setSelSchedule] = useState({});

    const user = React.useMemo(() => {
        if (cookies.user && typeof cookies.user === 'string') {
            try {
                return JSON.parse(cookies.user);
            } catch (e) {
                return null;
            }
        }
        return cookies.user;
    }, [cookies.user]);


    useEffect(() => {
        if (fileUploadDone) {
            filesUploaded();
            setFileUploadDone(false);
        }
    });

    const handleAlertClose = () => {
        setAlertHeader("");
        setAlertMessage("");
        setAlertOpen(false);
    }
    const handleLoginAlertClose = () => {
        setNotLoginError(false);
    }

    const filesUploaded = async () => {
        if (!user) {
            setNotLoginError(true);
            return;
        }
        
        const urlStr2 = `/api/file/by?username=${user.username}`;
        fetch(urlStr2, {
            headers: {
                Authorization: `Bearer ${user.token}`,
            },
        }).then(async resp => {
            const data = await resp.json();

            if (data.status >= 400) {
                setAlertHeader("Error");
                setAlertMessage(data.data || "Could not fetch files.");
                setAlertOpen(true);
            } else {
                setUserFiles(Array.isArray(data.data) ? data.data : []);
                setShowFiles(true);
            }
        }).catch(err => {
            console.error("Failed to fetch files:", err);
            setAlertHeader("Network Error");
            setAlertMessage("Could not connect to the server to get files.");
            setAlertOpen(true);
        });
    }

    async function deleteFile(filename) {
        const urlStr = `${API_FILE_PATH}/delete/${filename}?userId=${user.username}`;
        await fetch(urlStr, {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${user.token}`,
            },
        });
        filesUploaded();
    }
    
    const openDialog = (file) => {
        setFileToShare(file);
        setDialogOpen(true);
    }

    // const openScheduleDialog = (file) => {
    //     setSelSchedule(file.schedule);
    //     if (file.schedule && file.schedule.id) {
    //         scheduleContext.schedule.id = file.schedule.id;
    //     }
    //     setFileToShare(file.filename);
    //     setScheduleDialogOpen(true);
    // }
    // const cancelAndCloseScheduleDialog = (e) => {
    //     setScheduleDialogOpen(false);
    // }
    // const scheduleDialogClose = (e) => {
    //     setScheduleDialogOpen(false);

    //     const sendDate = scheduleContext.schedule.date.year() +
    //     '-' + (scheduleContext.schedule.date.month()+1) + 
    //     '-' + scheduleContext.schedule.date.date();
        
    //     const postObj = {
    //         "sendDate": sendDate,
    //         "receivers": [scheduleContext.schedule.to],
    //         "senderEmail": user.username,
    //         "senderName": user.google ? user.google.name: user.name,
    //         "isRecurring": false,
    //         "filename": fileToShare
    //     };
    //     let url = '/api/schedule/';
    //     let method = "POST";
    //     if (scheduleContext.schedule.id) {
    //         url = '/api/schedule/'+scheduleContext.schedule.id;
    //         method = "PUT"
    //     }
    //     fetch(url, {
    //         method: method,
    //         headers: {
    //             Authorization: `Bearer ${user.token}`,
    //             "Content-Type": "application/json",
    //         },
    //         body: JSON.stringify(postObj)
    //     }).then(resp => {
    //         console.log(resp);
    //     });
    // }

    const generatePublicLink = async (filename) => {
        try {

            const resp = await fetch(
                `/api/link/generate?username=${user.username}&filename=${filename}`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${user.token}`
                    }
                }
            )

            const link = await resp.text()

            await navigator.clipboard.writeText(link)

            alert("Public link copied:\n" + link)

        } catch (error) {
            console.error("Error generating public link", error)
        }
    }

    const getSendDateStr = (date) => {
        return date.split('T')[0];
    }

    const closeDialog = () => {
        setDialogOpen(false);
    }

    const shareFile = (e) => {
        setShareBtnDisabled(true);
        setShareBtnTxt("Sharing...");
        e.preventDefault();
        const emailAdd = document.getElementById("shareEmailId").value;
        const filename = `${user.username}/${fileToShare}`;
        const msg = `${user.name} shared ${fileToShare} with you`;
        const emailBody = {
            to: emailAdd,
            cc: [],
            bcc: [],
            subject: msg,
            body: `Dear ${emailAdd}, kindly download the attachment`,
            filesToAttach: [filename],
        };
        fetch("/api/social/sendMail", {
            method: "POST",
            headers: {
                Authorization: `Bearer ${user.token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(emailBody),
        }).then(() => {
            setDialogOpen(false);
            setShareBtnDisabled(false);
            setShareBtnTxt("Share");
        }).catch(e => {
            console.log(`Error occured ${e}`);
            console.log(JSON.stringify(e));
            setDialogOpen(false);
            setShareBtnDisabled(false);
            setShareBtnTxt("Share");
        });
    }
    const hideFiles = () => {
        setShowFiles(false);
    }
    const viewFile = async(file) => {
        const url = `/api/file/${file}/view?userId=${user.username}`;

        try {

            const resp = await fetch(url, {
                headers: {
                    Authorization: `Bearer ${user.token}`
                }
            });

            const presignedUrl = await resp.text();

            // redirect browser to S3
            window.location.href = presignedUrl;

        } catch (err) {
            console.error("Download error", err);
        }
    }

    return (
        <Container sx={{
            marginLeft: "auto",
            marginRight: "auto"
        }}>
            <Box>
                {user && showFiles ? (
                    <Box p={2} gap={2} sx={{
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                        overflow: "scroll",
                        boxShadow: "3px 1px 5px 5px grey",
                        borderRadius: 8,
                        marginLeft: "auto",
                        marginRight: "auto",
                        marginBottom: 5
                    }}>
                        <Box className="Flex-column-layout">
                            <Button variant="contained"
                                onClick={hideFiles}
                                sx={{ marginBottom: 2 }}
                                data-test="hideFiles">
                                Hide
                            </Button>
                            <Typography component={'span'} >
                                Files Uploaded by <Chip label={user.name ?? user.username}></Chip>
                            </Typography>
                        </Box>
                        <Divider orientation="horizontal" flexItem />
                        
                        {userFiles.length > 0 ? userFiles.map((file) => (
                            <Box key={file.filename} className="Flex-column">
                                <Box>
                                    <Typography component={'span'} variant="body"><b>{file.filename}</b>
                                    </Typography>
                                    
                                    {file.schedule ? 
                                    <p>Schedule: {getSendDateStr(file.schedule.sendDate)}</p>
                                    : <></> }
                                </Box>
                                <Box>
                                    <Button size="small"
                                        onClick={() => viewFile(file.filename)}> View </Button>
                                    <Button size="small"
                                        onClick={() => deleteFile(file.filename)}> Delete </Button>
                                    <Button size="small"
                                        onClick={() => openDialog(file.filename)}> Share </Button>
                                    <Button size="small"
                                        onClick={()=> generatePublicLink(file.filename)}> Public Link </Button>
                                </Box>
                            </Box>
                        )) : <Typography>No files found.</Typography>}
                    </Box>
                ) : (
                    <Box gap={2} sx={{ marginBottom: 5 }}>
                        <Button
                            data-test="showFiles"
                            variant="contained"
                            size="small"
                            onClick={filesUploaded}
                            disabled={user == null}>
                            Show
                        </Button>
                    </Box>
                )}
            </Box>

            <AlertDialog open={alertOpen}
                handleClose={handleAlertClose}
                title={alertHeader}
                content={alertMessage} />

            <AlertDialog open={notLoginErr}
                handleClose={handleLoginAlertClose}
                title="Not Logged in"
                content="You need to login first before using this" />


            <Dialog
                open={dialogOpen}
                onClose={closeDialog}
                PaperProps={{
                    component: 'form',
                    onSubmit: shareFile
                }}>
                <DialogTitle> Share File</DialogTitle>
                <DialogContent className="Flex-column-layout">
                    <Alert variant="outlined" severity="info">
                        Only 1 email address supported
                    </Alert>
                    <TextField autoFocus
                        fullWidth
                        required
                        margin="dense"
                        label="Email address to share with"
                        placeholder="Enter that person's email address"
                        type="email"
                        id="shareEmailId"
                    />
                </DialogContent>

                <DialogActions>
                    <Button variant="contained"
                        size="small"
                        onClick={closeDialog}>Close</Button>
                    <Button variant="contained"
                        disabled={shareBtnDisabled}
                        size="small"
                        type="submit">{shareBtnTxt}</Button>
                </DialogActions>
            </Dialog>
        </Container >
    )
}