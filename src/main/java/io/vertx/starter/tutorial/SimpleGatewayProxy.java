package io.vertx.starter.tutorial;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class SimpleGatewayProxy extends AbstractVerticle {
	
	


	// Convenience method so you can run it in your IDE
	public static void main(String[] args) {

		VertxOptions vOpts = new VertxOptions();
		DeploymentOptions options = new DeploymentOptions().setInstances(1).setHa(true)
				.setConfig(new JsonObject().put("local", false));
		vOpts.setClustered(true).setClusterHost("192.168.0.106");
		Vertx.clusteredVertx(vOpts, cluster -> {
			if (cluster.succeeded()) {
				final Vertx result = cluster.result();
				result.deployVerticle(SimpleGatewayProxy.class.getName(), options, handle -> {

				});
			}
		});

		/*
		 * vertx = Vertx.vertx();
		 * 
		 * /* VertxOptions options = new VertxOptions().setClustered(true)
		 * .setClusterHost("192.168.0.106") .setHAEnabled(true) .setHAGroup("dev");
		 * 
		 * Vertx.clusteredVertx(options, res -> System.out.println(res.succeeded()));
		 * 
		 * 
		 * VertxOptions options = new VertxOptions(); Vertx.clusteredVertx(options, res
		 * -> { if (res.succeeded()) {
		 * 
		 * EventBus eventBus = res.result().eventBus();
		 * System.out.println("We now have a clustered event bus: " + eventBus); } else
		 * { System.out.println("Failed: " + res.cause()); } });
		 * 
		 * vertx.deployVerticle(new SimpleRest());
		 */

		/**
		 * DeploymentOptions options = new DeploymentOptions().setInstances(1)
		 * .setConfig(new JsonObject().put("local", true).put("host",
		 * "0.0.0.0").put("port", 8181)); VertxOptions vOpts = new VertxOptions();
		 * vOpts.setClustered(true); Vertx.clusteredVertx(vOpts, cluster -> { if
		 * (cluster.succeeded()) { final Vertx result = cluster.result();
		 * result.deployVerticle(SimpleRest.class.getName(), options, handle -> {
		 * 
		 * }); } });
		 */
	}
	
	 private Schema schema;

	@Override
	public void start() {

		Router router = Router.router(vertx);

		router.route().handler(BodyHandler.create());
		router.post("/event/:eventType").handler(this::handlerEvent);

		vertx.createHttpServer().requestHandler(router).listen(8080);
		
		
		
		
		try (InputStream inputStream = getClass().getResourceAsStream("event_schema.json")) {
			  JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
			  schema = SchemaLoader.load(rawSchema);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage());
		}
		
	}

	private void handlerEvent(RoutingContext routingContext) {
		String eventType = routingContext.request().getParam("eventType");
		System.out.println(eventType);

		try {
			
			
			//validar JSON EVENT
			String strJSON = routingContext.getBodyAsString() ; 
			List<String> violations=  validateJSON(strJSON);
			if(violations!=null && violations.size() > 0 ) {
				JsonObject jsonResult = new JsonObject();
				jsonResult.put("error" , "violation type event");
				jsonResult.put("messagens" , violations);
				
				routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
				routingContext.response().end(jsonResult.encode());
				
			}
			
			JsonObject newEvent = routingContext.getBodyAsJson();
			
			vertx.eventBus().send(eventType, newEvent,
					(Handler<AsyncResult<Message<JsonObject>>>) responseHandler -> defaultResponse(routingContext,
							responseHandler));
		} catch (RuntimeException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			routingContext.fail(500);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			routingContext.fail(500);
		}

	
	}

	private void defaultResponse(RoutingContext routingContext, AsyncResult<Message<JsonObject>> responseHandler) {

		if (responseHandler.failed()) {
			
			JsonObject jsonERROR = new JsonObject();
			jsonERROR.put("error" , responseHandler.cause().getMessage());
			jsonERROR.put("trace" , responseHandler.cause().toString());			
			routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			routingContext.response().end(jsonERROR.encode());
			
			
		} else {
			final Message<JsonObject> result = responseHandler.result();
			routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			routingContext.response().end(result.body().encode());
		}
	}

	
	private List<String>  validateJSON(String strJSON) throws IOException {
		
		List<String> violations =  new ArrayList<>();
		 
		  try {
			  JSONObject jsonOBJ = new JSONObject(strJSON) ; 
			  schema.validate(jsonOBJ); 
			} catch (ValidationException e) {
			
			  System.out.println(e.getMessage());
			  e.getCausingExceptions().stream()
		      .map(ValidationException::getMessage)
		      .forEach(System.out::println);
			  
			  violations = e.getAllMessages(); 
			}catch (org.json.JSONException exJSON) {
				
				violations.add(exJSON.getMessage());
				
			}
		return violations; 
		
		
	}
	
	
	/***
	 * {
  "eventType": "createUser",
  "eventUUID": "123333",
  "payload": {}
}
	 */

}