package org.endeavourhealth.messagingapi.endpoints;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkBaseException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.http.entity.ContentType;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.datasharingmanagermodel.models.database.DataProcessingAgreementEntity;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.List;

@Path("/")
public class PostFile extends AbstractEndpoint {
	@POST
	@Path("/PostFile")
	//@RolesAllowed({"tpp-bulk-extract-provider", "homerton-bulk-extract-provider"})
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFiles(@Context final HttpServletRequest request) {
		String organisationId = request.getQueryString().replaceAll("organisationId=","");

		// Check that we have a multi-part file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (isMultipart) {
			// Create a factory for temp disk file items
			DiskFileItemFactory factory = new DiskFileItemFactory();
			// Configure a repository (to ensure a secure temp location is used)
			ServletContext servletContext = request.getServletContext();
			File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
			factory.setRepository(repository);
			// Setup temp disk clearing tracker
			FileCleaningTracker fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(servletContext);
			factory.setFileCleaningTracker(fileCleaningTracker);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);
			try {
				// Parse and upload the files
				List<FileItem> items = upload.parseRequest(request);

				// Check the publisher has a DPA with Discovery before moving the data onto AWS
				if (publisherHasDPA(organisationId)) {
					MoveDataFilesToAWS(organisationId, items);
				}
				else {
					return Response.serverError().status(Response.Status.METHOD_NOT_ALLOWED).entity(String.format("No DPA for publishing organisation: %s",organisationId)).build();
				}
			} catch (Exception e) {
				return Response.serverError().status(Response.Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
			}
			return Response.ok().status(Response.Status.OK).entity("Data file upload complete").build();
		}
		return Response.serverError().status(Response.Status.METHOD_NOT_ALLOWED).entity("Unexpected content. Multi-part expected").build();
	}

	private static void MoveDataFilesToAWS(String organisationId, List<FileItem> fileItems) throws Exception {
		{
			JsonNode apiAWSConfig;
			try {
				apiAWSConfig = ConfigManager.getConfigurationAsJson("aws", "messaging-api");
			}
			catch (Exception ex)
			{
				throw new Exception("Failed to get messaging-api configuration for aws",ex);
			}

			String awsBucketName = apiAWSConfig.findValue("s3-bucket").textValue();
			String awsAccessKeyId = apiAWSConfig.findValue("access-key-id").textValue();
			String awsSecretKey = apiAWSConfig.findValue("secret-access-key").textValue();
			String awsRegion = apiAWSConfig.findValue("region").textValue();
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey);
			ClientConfiguration clientConfig = new ClientConfiguration();
			clientConfig.setConnectionTimeout(7200000); clientConfig.setSocketTimeout(7200000); clientConfig.setRequestTimeout(7200000);
			clientConfig.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(5));

			AmazonS3ClientBuilder s3 = AmazonS3ClientBuilder.standard()
					.withClientConfiguration(clientConfig)
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
					.withRegion(awsRegion);
			TransferManagerBuilder tx = TransferManagerBuilder.standard().withS3Client(s3.build());
			try {
				for (FileItem fileItem: fileItems) {
					if (!fileItem.isFormField()) {
						// get the file stream from the temp file on disk
						InputStream uploadedInputStream = fileItem.getInputStream();
						ObjectMetadata metaData = new ObjectMetadata();
						metaData.setContentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
						long sizeInBytes = fileItem.getSize();
						metaData.setContentLength(sizeInBytes);

						// create the AWS object key from the inbound filename
						String keyPath = createFileKey(organisationId, fileItem.getName());
						System.out.println("keyPath: " + keyPath + " available received bytes: " + sizeInBytes);

						// upload the file stream to AWS
						try {
							Upload upload = tx.build().upload(awsBucketName, keyPath, uploadedInputStream, metaData);

							long megaBytes = -1;
							while (!upload.isDone()) {
								TransferProgress progress = upload.getProgress();
								double pct = progress.getPercentTransferred();
								long mBytes = progress.getBytesTransferred() / 1000000;
								if (megaBytes != mBytes) {
									megaBytes = mBytes;
									if (pct > 0)
										System.out.format(keyPath+": %.2f percent: ("+mBytes+" mb)",pct).println();
								}

								if (upload.getState() == Transfer.TransferState.Canceled)
								{
									uploadedInputStream.close();
									throw new SdkBaseException("Upload cancelled");
								}
							}

							uploadedInputStream.close();
							System.out.println("File: " + keyPath + " received and uploaded to AWS bucket: " + awsBucketName);
						}
						catch (Exception ex) {
							uploadedInputStream.close();
							ex.printStackTrace();
							throw ex;
						}
					}
				}
			} finally {
				tx.build().shutdownNow();
			}
		}
	}

	private static String createFileKey(String organisationId, String filePath) {
		return organisationId.concat("/data/" + filePath.replace("\\", "/"));
	}

	private static boolean publisherHasDPA(String organisationId)  {
		// check DPA in place for data publishing org
		try {
			List<DataProcessingAgreementEntity> matchingDpa = DataProcessingAgreementEntity.getDataProcessingAgreementsForOrganisation(organisationId);

			return matchingDpa.size()>0;
		}
		catch (Exception Ex) {
			return false;
		}
	}
}