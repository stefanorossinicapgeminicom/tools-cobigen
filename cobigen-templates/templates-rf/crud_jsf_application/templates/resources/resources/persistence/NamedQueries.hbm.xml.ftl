<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                                   "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">

<!-- ======================================================================
	This is the hibernate mapping file. It's found by JPA automatically. The
	mapping file is not intended for the mapping of entities. Entity-mappings
	are defined via annotations. This file is intended for query definitions.
	Whenever possible, the JPQL (Java Persistence Query Language) should be used
	for queries. Native SQL is only allowed for special cases, e.g. to give Oracle
	SQL Index Hints. Autor: Christian Hinken Revision: $Id: NamedQueries.hbm.xml
	532 2013-02-04 14:41:45Z etomety $ ====================================================================== -->
<hibernate-mapping default-lazy="true">

	<query name="get.all.${pojo.name?lower_case}s">
    	<![CDATA[SELECT t FROM ${pojo.name} t]]>
	</query>

</hibernate-mapping>
