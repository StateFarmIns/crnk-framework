package io.crnk.jpa.query;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.url.ConstantServiceUrlProvider;
import io.crnk.jpa.JpaModule;
import io.crnk.jpa.meta.JpaMetaProvider;
import io.crnk.jpa.model.BasicAttributesTestEntity;
import io.crnk.jpa.model.CountryEntity;
import io.crnk.jpa.model.CountryTranslationEntity;
import io.crnk.jpa.model.CustomTypeTestEntity;
import io.crnk.jpa.model.JoinedTableBaseEntity;
import io.crnk.jpa.model.JoinedTableChildEntity;
import io.crnk.jpa.model.JpaTransientTestEntity;
import io.crnk.jpa.model.LangEntity;
import io.crnk.jpa.model.ManyToManyOppositeEntity;
import io.crnk.jpa.model.ManyToManyTestEntity;
import io.crnk.jpa.model.OneToOneTestEntity;
import io.crnk.jpa.model.OtherRelatedEntity;
import io.crnk.jpa.model.OverrideIdTestEntity;
import io.crnk.jpa.model.RelatedEntity;
import io.crnk.jpa.model.RenamedTestEntity;
import io.crnk.jpa.model.SingleTableBaseEntity;
import io.crnk.jpa.model.SingleTableChildEntity;
import io.crnk.jpa.model.TablePerClassBaseEntity;
import io.crnk.jpa.model.TablePerClassChildEntity;
import io.crnk.jpa.model.TestAnyType;
import io.crnk.jpa.model.TestEmbeddable;
import io.crnk.jpa.model.TestEmbeddedIdEntity;
import io.crnk.jpa.model.TestEntity;
import io.crnk.jpa.model.TestIdEmbeddable;
import io.crnk.jpa.model.TestNestedEmbeddable;
import io.crnk.jpa.model.TestSubclassWithSuperclassPk;
import io.crnk.jpa.model.UuidTestEntity;
import io.crnk.jpa.query.criteria.JpaCriteriaQueryFactory;
import io.crnk.jpa.util.JpaTestConfig;
import io.crnk.jpa.util.SpringTransactionRunner;
import io.crnk.meta.MetaLookup;
import io.crnk.meta.provider.MetaPartition;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaTestConfig.class)
public abstract class AbstractJpaTest {

	@PersistenceContext
	protected EntityManager em;

	@Autowired
	protected EntityManagerFactory emFactory;

	protected JpaModule module;
	protected JpaQueryFactory queryFactory;

	protected int numTestEntities = 5;

	@Autowired
	protected PlatformTransactionManager txManager;
	protected ResourceRegistry resourceRegistry;
	@Autowired
	private SpringTransactionRunner transactionRunner;

	public static void clear(EntityManager em) {
		clear(em, JpaCriteriaQueryFactory.newInstance());
	}

	public static void clear(final EntityManager em, JpaQueryFactory factory) {
		factory.initalize(new JpaQueryFactoryContext() {
			@Override
			public EntityManager getEntityManager() {
				return em;
			}

			@Override
			public MetaPartition getMetaPartition() {
				JpaMetaProvider jpaMetaProvider = new JpaMetaProvider(em.getEntityManagerFactory());
				MetaLookup metaLookup = new MetaLookup();
				metaLookup.addProvider(jpaMetaProvider);
				metaLookup.initialize();
				return jpaMetaProvider.getPartition();
			}
		});
		clear(em, factory.query(OneToOneTestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(ManyToManyTestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(ManyToManyOppositeEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(TestSubclassWithSuperclassPk.class).buildExecutor().getResultList());
		clear(em, factory.query(RelatedEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(TestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(OtherRelatedEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(CountryTranslationEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(CountryEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(LangEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(BasicAttributesTestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(RenamedTestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(UuidTestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(JpaTransientTestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(CustomTypeTestEntity.class).buildExecutor().getResultList());
		clear(em, factory.query(OverrideIdTestEntity.class).buildExecutor().getResultList());
		em.flush();
		em.clear();
	}

	private static void clear(EntityManager em, List<?> list) {
		for (Object obj : list) {
			em.remove(obj);
		}
	}

	@Before
	public void setup() {
		CrnkBoot boot = new CrnkBoot();

		boot.setServiceUrlProvider(new ConstantServiceUrlProvider("http://localhost:1234"));
		module = JpaModule.newServerModule(emFactory, em, transactionRunner);
		setupModule(module);
		boot.addModule(module);
		boot.boot();
		resourceRegistry = boot.getResourceRegistry();

		queryFactory = createQueryFactory(em);
		module.setQueryFactory(queryFactory);


		clear();
		for (int i = 0; i < numTestEntities; i++) {

			TestEmbeddedIdEntity idEntity = new TestEmbeddedIdEntity();
			idEntity.setId(new TestIdEmbeddable(i, "test" + i));
			idEntity.setLongValue(100L + i);
			em.persist(idEntity);

			RelatedEntity related = new RelatedEntity();
			related.setId(100L + i);
			related.setStringValue("related" + i);
			em.persist(related);

			TestAnyType anyValue = new TestAnyType();
			if (i == 0)
				anyValue.setValue("first");
			else
				anyValue.setValue(i);

			TestEmbeddable embValue = new TestEmbeddable();
			embValue.setEmbIntValue(i);
			embValue.setEmbStringValue("emb" + i);
			embValue.setNestedValue(new TestNestedEmbeddable(i == 0));
			embValue.setAnyValue(anyValue);

			TestEntity test = new TestEntity();
			test.setStringValue("test" + i);
			test.setId((long) i);
			test.setLongValue(i);
			test.setEmbValue(embValue);

			// do not include relation/map for last value to check for proper
			// left join sorting
			if (i != numTestEntities - 1) {
				test.setOneRelatedValue(related);
				test.getMapValue().put("a", "a" + i);
				test.getMapValue().put("b", "b" + i);
				test.getMapValue().put("c", "c" + i);
			}
			em.persist(test);

			// inheritance
			SingleTableBaseEntity singleTableBase = new SingleTableBaseEntity();
			singleTableBase.setId((long) i);
			singleTableBase.setStringValue("base" + i);
			em.persist(singleTableBase);
			SingleTableChildEntity singleTableChild = new SingleTableChildEntity();
			singleTableChild.setId((long) i + numTestEntities);
			singleTableChild.setStringValue("child" + i);
			singleTableChild.setIntValue(i);
			em.persist(singleTableChild);

			JoinedTableBaseEntity joinedTableBase = new JoinedTableBaseEntity();
			joinedTableBase.setId((long) i);
			joinedTableBase.setStringValue("base" + i);
			em.persist(joinedTableBase);
			JoinedTableChildEntity joinedTableChild = new JoinedTableChildEntity();
			joinedTableChild.setId((long) i + numTestEntities);
			joinedTableChild.setStringValue("child" + i);
			joinedTableChild.setIntValue(i);
			em.persist(joinedTableChild);

			TablePerClassBaseEntity tablePerClassBase = new TablePerClassBaseEntity();
			tablePerClassBase.setId((long) i);
			tablePerClassBase.setStringValue("base" + i);
			em.persist(tablePerClassBase);
			TablePerClassChildEntity tablePerClassChild = new TablePerClassChildEntity();
			tablePerClassChild.setId((long) i + numTestEntities);
			tablePerClassChild.setStringValue("child" + i);
			tablePerClassChild.setIntValue(i);
			em.persist(tablePerClassChild);
		}
		em.flush();
		em.clear();

		queryFactory = createQueryFactory(em);
		module.setQueryFactory(queryFactory);
	}

	/**
	 * Implement this to switch between Criteria und QueryDSL.
	 */
	protected abstract JpaQueryFactory createQueryFactory(EntityManager em);

	protected void setupModule(JpaModule module2) {
	}

	private void clear() {
		clear(em, createQueryFactory(em));
	}

}
