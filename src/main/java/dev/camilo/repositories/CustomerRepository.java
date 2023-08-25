package dev.camilo.repositories;

import dev.camilo.entities.Customer;
import dev.camilo.entities.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CustomerRepository {
  @Inject
  EntityManager em;

  @Transactional
  public void createdCustomer(Customer customer){
    em.persist(customer);
  }
  @Transactional
  public void deleteCustomer(Customer customer){
    em.remove(em.contains(customer) ? customer : em.merge(customer));
  }
  @Transactional
  public List<Customer> listCustomer(){
    List<Customer> customers = em.createQuery("select p from Customer p").getResultList();
    return customers;
  }
  @Transactional
  public Customer findCustomer(Long id){
    return em.find(Customer.class, id);
  }
  @Transactional
  public Customer updateCustomer(Long id, Customer customer) {
    Customer existingCustomer = findCustomer(id);
    if (existingCustomer == null) {
      throw new RuntimeException("Customer with ID " + id + " not found");
    }

    // Actualizar los campos del Customer con los nuevos valores
    existingCustomer.setNames(customer.getNames());
    existingCustomer.setAccountNumber(customer.getAccountNumber());
    existingCustomer.setSurename(customer.getSurename());
    existingCustomer.setPhone(customer.getPhone());
    existingCustomer.setAddress(customer.getAddress());

    // Cargar la lista actual de productos asociados al Customer
    List<Product> existingProducts = existingCustomer.getProducts();

    // Iterar por la lista de productos proporcionada en la solicitud
    for (Product newProduct : customer.getProducts()) {
      // Si el producto ya existe en la base de datos, actualizar su referencia
      if (newProduct.getId() != null) {
        Product existingProduct = em.find(Product.class, newProduct.getId());
        if (existingProduct != null) {
          existingProduct.setCustomer(existingCustomer);
        }
      } else {
        // Si el producto es nuevo, persistirlo y establecer la referencia
        newProduct.setCustomer(existingCustomer);
        em.persist(newProduct);
      }
    }

    // Actualizar el Customer y retornar el resultado
    return em.merge(existingCustomer);
  }

}