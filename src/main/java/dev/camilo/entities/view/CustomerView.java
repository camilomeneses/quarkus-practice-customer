package dev.camilo.entities.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import dev.camilo.entities.jpa.Customer;
import dev.camilo.entities.jpa.Product;

import java.util.List;

@EntityView(Customer.class)
public interface CustomerView {

  @IdMapping
  Long getId();
  String getAccountNumber();
  String getNames();
  String getSurname();
  String getPhone();
  String getAddress();
  List<Product> getProducts();
}
