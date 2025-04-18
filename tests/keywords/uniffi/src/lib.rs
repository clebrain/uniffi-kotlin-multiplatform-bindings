/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::{collections::HashMap, sync::Arc};

pub fn r#if(_object: u8) {}

#[allow(non_camel_case_types)]
pub struct r#break {}

impl r#break {
    pub fn class(&self, _object: u8) {}
    pub fn object(&self, _class: Option<u8>) {}
}

#[allow(non_camel_case_types)]
trait r#continue {
    fn r#return(&self, v: r#return) -> r#return;
    // fn r#continue(&self, v: Vec<Box<dyn r#continue>>) -> Option<Box<dyn r#continue>>;
    fn r#break(&self, _v: Option<Arc<r#break>>) -> HashMap<u8, Arc<r#break>>;
    fn r#while(&self, _v: Vec<r#while>) -> r#while;
    // fn class(&self, _v: HashMap<u8, Vec<class>>) -> Option<HashMap<u8, Vec<class>>>;
}

#[allow(non_camel_case_types)]
pub struct r#return {
    class: u8,
    object: Option<u8>,
}

#[allow(non_camel_case_types)]
pub struct r#while {
    class: r#return,
    fun: Option<r#return>,
    object: Vec<r#return>,
    r#break: HashMap<u8, r#return>,
}

#[allow(non_camel_case_types)]
pub enum r#false {
    #[allow(non_camel_case_types)]
    r#true { object: u8 },
}

#[allow(non_camel_case_types)]
#[derive(Debug, thiserror::Error)]
pub enum class {
    #[error("object error")]
    object,
}

#[allow(non_camel_case_types)]
#[derive(Debug, thiserror::Error)]
pub enum fun {
    #[error("class?")]
    class { object: u8 },
}

uniffi::include_scaffolding!("keywords");
uniffi_reexport_scaffolding!();