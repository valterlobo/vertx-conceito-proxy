package io.vertx.starter.tutorial;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;

public class UserHandlerEvent extends AbstractVerticle {

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		vertx.eventBus().consumer("getUsers", getUsers());

		vertx.eventBus().consumer("createUser", createUser());

		vertx.eventBus().consumer("testERROR", testERROR());

	}

	/***
	 * TODO HANDLER REALIZAR A CAPTURA DE EXECEPTION E RETORNAR A FALHA FAZER
	 * METODOS TEMPLATES .
	 * 
	 * @return
	 */

	private Handler<Message<JsonObject>> testERROR() {

		return handler -> {

			try {

				System.out.print("handler -> testERROR");

				throw new IllegalStateException("ERRO TESTE _ COMO REALIZAR O TRATAMENTO ? ");

			} catch (RuntimeException runex) {

				JsonObject jsonERROR = new JsonObject();
				jsonERROR.put("error", runex.getMessage());
				jsonERROR.put("trace", runex.getCause()!=null?runex.getCause().toString():" ");
				handler.reply(jsonERROR);

			}

		};
	}

	private Handler<Message<JsonObject>> createUser() {

		return handler -> {
			System.out.print("handler -> createUser");
			final JsonObject body = handler.body();

			System.out.println(body);

			if (body == null) {

				handler.fail(404, "user does not exists");
			} else {
				handler.reply(body);

			}
		};
	}

	private Handler<Message<JsonObject>> getUsers() {
		System.out.print("handler -> getUsers");
		return handler -> {
			final JsonObject body = handler.body();

			if (body == null) {

				handler.fail(404, "user does not exists");
			} else {
				handler.reply(body);

			}
		};
	}

	// Convenience method so you can run it in your IDE
	public static void main(String[] args) {
		Vertx.clusteredVertx(new VertxOptions().setClustered(true), cluster -> {
			if (cluster.succeeded()) {
				final Vertx result = cluster.result();
				result.deployVerticle(UserHandlerEvent.class.getName(),
						new DeploymentOptions().setConfig(new JsonObject().put("local", true)), handle -> {

						});
			}
		});
	}

}
